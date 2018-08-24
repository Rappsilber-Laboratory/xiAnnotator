/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rappsilber;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.io.IOException;
import java.io.InputStream;

/**
 * just reads json requests from stdin and outputs the result to stdout.
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class StreamAnnotate {
    public static InputStream in = System.in;
    public static void main(String[] args) throws IOException {
        xiAnnotator a = new xiAnnotator();
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = (char) in.read();
            sb.append(c);
            if (c == '}') {
                Gson gson = new Gson();
                try {
                    LinkedTreeMap result = gson.fromJson(sb.toString() , LinkedTreeMap.class);    
                    System.out.print(a.getFullAnnotation(sb.toString()).getEntity());
                    sb.setLength(0);
                } catch (Exception e) {

                }
            }
        }
    }
    
}
