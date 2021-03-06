package com.sandbox.runtime.js.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandbox.runtime.models.Cache;
import jdk.nashorn.internal.objects.NativeArray;
import jdk.nashorn.internal.runtime.ScriptObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by drew on 4/08/2014.
 */
public class NashornRuntimeUtils implements INashornUtils{

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Cache cache;

    private static Logger logger = LoggerFactory.getLogger(NashornRuntimeUtils.class);

    private final String fullSandboxId;

    public NashornRuntimeUtils(String fullSandboxId) {
        this.fullSandboxId = fullSandboxId;
    }

    public String getFullSandboxId() { return fullSandboxId; }

    public String doJsonStringify(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            logger.error("Error in jsonStringify with obj: " + o,e);
            return null;
        }
    }

    //dumb but necessary because of nashorn's auto magic typing nonsense.
    public String jsonStringify(Object o) {
        return doJsonStringify(o);
    }

    public String jsonStringify(ScriptObject o) {
        return doJsonStringify(o);
    }

    public String jsonStringify(NativeArray o) {
        return doJsonStringify(o);
    }

    public String readFile(String filename) {
        return cache.getRepositoryFile(fullSandboxId, filename);
    }

    public boolean hasFile(String filename) {
        logger.debug("hasFile ({}) - {}", fullSandboxId,filename);
        return cache.hasRepositoryFile(fullSandboxId, filename);
    }
}
