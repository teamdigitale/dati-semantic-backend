package it.gov.innovazione.ndc.harvester.harvesters.utils;

import it.gov.innovazione.ndc.harvester.context.HarvestExecutionContext;

import java.io.File;

public class PathUtils {

    public static String relativizeFolder(String ttlFile, HarvestExecutionContext context) {
        return getFolderNameFromFile(ttlFile).replace(
                context.getRootPath(), "");
    }

    public static String relativizeFile(String ttlFile, HarvestExecutionContext context) {
        return ttlFile.replace(
                context.getRootPath(), "");
    }

    private static String getFolderNameFromFile(String ttlFile) {
        return new File(ttlFile).getParent();
    }
}
