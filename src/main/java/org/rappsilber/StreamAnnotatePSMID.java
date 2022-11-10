/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rappsilber;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * just reads PSMIDs from stdin and outputs the result to stdout.
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class StreamAnnotatePSMID {
    public static InputStream in = System.in;
    public static void main(String[] args) throws IOException, SQLException, FileNotFoundException, ParseException {
        xiAnnotator a = new xiAnnotator();
        Connection con = a.getConnection();
        // get search id from PSM ID
        
        StringBuilder sb = new StringBuilder();
        while (true) {
            int i =  in.read();
            char c = (char) i;
            sb.append(c);
            if ((c == '\n' || c == '\r' || i==-1) & sb.length() > 0) {
                long psmid = Long.parseLong(sb.toString().replaceAll("[\\n\\r\\s]", ""));
                System.err.println("PSM ID:" + psmid);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT search_id FROM spectrum_match WHERE id = " + psmid);
                rs.next();
                System.err.println("isAfterLast " + rs.isAfterLast());
                Integer Search_id = rs.getInt(1);
                Gson gson = new Gson();
                try {
                    System.out.print(a.getAnnotation(Search_id, "", psmid, new ArrayList<String>(), new ArrayList<Integer>(), new ArrayList<String>(), 0, true).getEntity());
                    sb.setLength(0);
                } catch (Exception e) {
                    System.err.println("error:" + e);
                    e.printStackTrace(System.err);
                }
            }
            if (i == -1)
                return;
        }
    }
    
}
