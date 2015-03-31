package de.fhwedel.google.cardboardprojekt.gl;

import android.content.res.AssetManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

public class ShaderCodeLoader {

    public static String readSourceFile(String pathToSource, AssetManager assets) {
        Log.i("ShaderCodeLoader", String.format("Parsing File: %s", pathToSource));
        String codeInOneString = "";
        try {

            List<String> lines = IOUtils.readLines(assets.open(pathToSource));

            for (String line : lines) {
                codeInOneString += line + "\n";
            }

        } catch (IOException e) {
            Log.e("ShaderCodeLoader", String.format("could not read file: %s", pathToSource));
        }

        Log.d("ShaderCodeLoader", codeInOneString);

        Log.i("ShaderCodeLoader", String.format("Finished parsing File: %s", pathToSource));
        return codeInOneString;
    }
}
