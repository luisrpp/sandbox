package com.sandbox.runtime.js.services;

import com.sandbox.runtime.js.models.Console;
import com.sandbox.runtime.js.utils.FileUtils;
import com.sandbox.runtime.js.utils.NashornRuntimeUtils;
import com.sandbox.runtime.models.SandboxScriptEngine;
import com.sandbox.runtime.utils.GenericEngineQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * Created by nickhoughton on 8/08/2014.
 */
public class JSEngineQueue extends GenericEngineQueue {

    static Logger logger = LoggerFactory.getLogger(JSEngineQueue.class);

    public JSEngineQueue(int targetInQueue, ApplicationContext context) {
        super(context, targetInQueue);
    }

    //executed once when the engine is created
    @Override
    protected SandboxScriptEngine initializeEngine(SandboxScriptEngine sandboxEngine){
        logger.debug("Initializing engine..");
        //noop not really needed anymore, because nashorn now has a code cache we can do it in the prepare stage and use the same context, simplifies things.

        return sandboxEngine;
    }

    private void loadAndSealScript(String name, String file, String objectName, Bindings scope, ScriptEngine engine) throws ScriptException {
        scope.put(ScriptEngine.FILENAME, name);
        engine.eval(FileUtils.loadJSFromResource(file), scope);
        engine.eval("Object.freeze(" + objectName + "); Object.seal(" + objectName + ");", scope);
    }

    //this is the executed per request, so everytime the engine goes back into the queue this runs to clear any junk the user might have left
    @Override
    protected SandboxScriptEngine prepareEngine(SandboxScriptEngine sandboxEngine){
        Console consoleInstance = context.getBean(Console.class);
        sandboxEngine.setConsole(consoleInstance);

        NashornRuntimeUtils nashornRuntimeUtils = (NashornRuntimeUtils) context.getBean("nashornUtils","temporary");

        final Bindings engineScope = new SimpleBindings();
        final ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(engineScope, ScriptContext.ENGINE_SCOPE);
        ctx.setAttribute("_console", sandboxEngine.getConsole(), ScriptContext.ENGINE_SCOPE);

        ctx.setAttribute("nashornUtils", nashornRuntimeUtils, ScriptContext.ENGINE_SCOPE);

        try {
            loadAndSealScript("lodash-2.4.1.js","lib/lodash-2.4.1.min", "_", engineScope, sandboxEngine.getEngine());
            loadAndSealScript("faker.js","lib/faker-2.1.2.min", "faker", engineScope, sandboxEngine.getEngine());
            loadAndSealScript("moment.js", "lib/moment-2.8.2.min", "moment", engineScope, sandboxEngine.getEngine());
            loadAndSealScript("amanda.js", "lib/amanda-0.4.8.min", "amanda", engineScope, sandboxEngine.getEngine());
            loadAndSealScript("validator.js", "lib/validator.min", "validator", engineScope, sandboxEngine.getEngine());
            loadAndSealScript("sandbox-validator.js", "sandbox-validator", "sandboxValidator", engineScope, sandboxEngine.getEngine());
        } catch (ScriptException e) {
            logger.error("Error loading 3rd party JS", e);
        }

        // monkey patch nashorn
        try {
            engineScope.put(ScriptEngine.FILENAME, "<sandbox-internal>");
            sandboxEngine.getEngine().eval(FileUtils.loadJSFromResource("sandbox-patch"), ctx);
        } catch (ScriptException e) {
            logger.error("Error postProcessing engine",e);
        }

        sandboxEngine.setContext(ctx);

        return sandboxEngine;
    }

    @Override
    protected SandboxScriptEngine postProcessEngine(SandboxScriptEngine sandboxEngine) {

        return sandboxEngine;
    }

}
