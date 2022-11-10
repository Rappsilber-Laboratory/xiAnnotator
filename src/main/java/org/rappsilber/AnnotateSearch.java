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
import javax.ws.rs.core.Response;
import rappsilber.ms.ToleranceUnit;

/**
 * just reads PSMIDs from stdin and outputs the result to stdout.
 * @author Lutz Fischer <lfischer@staffmail.ed.ac.uk>
 */
public class AnnotateSearch {
    public static InputStream in = System.in;
    public static void main(String[] args) throws IOException, SQLException, FileNotFoundException, ParseException {
        if (args.length==0)
            args = new String[]{"17102"};
            
        xiAnnotator a = new xiAnnotator();
        Connection con = a.getConnection();
        Integer searchid = Integer.parseInt(args[0]);
        // get search id from PSM ID
        Statement st = con.createStatement();
        String where = " WHERE sm.search_id = " + args[0] + " and sm.dynamic_rank";
        String querryCount = "SELECT count(*) FROM spectrum_match sm" + where;
        String querryPSMid = "SELECT sm.id, score, sps.name,s.scan_number FROM"
                + " spectrum_match sm inner join"
                + " spectrum s on sm.spectrum_id = s.id inner join"
                + " spectrum_source sps on s.source_id = sps.id" + where;
        System.err.println("requesting list of match IDs");
        ResultSet rs = st.executeQuery(querryCount);
        rs.next();
        Long maxcount = rs.getLong(1);
        rs.close();
        rs = st.executeQuery(querryPSMid);
        System.err.println(maxcount + " matches");
        System.err.println("going through IDs");
        System.out.print("psmid, score, rawfile, scan, P+P found, ");
        System.out.print("expMZ, expPeakFound, expPeakIntensity, relExpPeakIntensity, ");
        System.out.print("calcMZ, calcPeakFound, calcPeakIntensity, relCalcPeakIntensity/maxIntensity, " );
        System.out.print("isdecoy, std, dcount, noncovalent, ");
        System.out.print("noncovalent exp found, noncovalent calc found, ");
        System.out.print("covalent exp found, covalent calc found, ");
        System.out.print("noncovalent corrected found, covalent corrected found , ");
        System.out.println();
        
        long count=0;
        long old_percent = 0;
        while (rs.next()) {
            count++;
            Long psmid= rs.getLong(1);
            Double score = rs.getDouble(2);
            String rawfile = rs.getString(3);
            String scan = rs.getString(4);
            //System.err.println("PSM ID:" + psmid);
            if (count%100 ==0 || count*100/maxcount >old_percent) {
                old_percent = count*100/maxcount;
                System.err.print(" -- " + count + " / " + maxcount + " ("+old_percent + "%)\r");
            }
            Gson gson = new Gson();
            // get the annotation 
            String s = a.getAnnotation(searchid, "", psmid, new ArrayList<String>(), new ArrayList<Integer>(), new ArrayList<String>(), 0, true).getEntity().toString();
            try {
                final LinkedTreeMap result = gson.fromJson(s , LinkedTreeMap.class);        
                // is P+P matched ?
                boolean hasPP = s.contains("\"P+P\"");
                // annotation part of the json
                final LinkedTreeMap annotation = ((LinkedTreeMap)result.get("annotation"));
                // all peak data - needed to find experimantal and calculated precursor peaks - independet of annotation
                ArrayList<LinkedTreeMap> peaks = (ArrayList<LinkedTreeMap>) result.get("peaks");
                // peptides - mainly needed to figure out TT TD or DD
                ArrayList<LinkedTreeMap> peptides = (ArrayList<LinkedTreeMap>) result.get("Peptides");
                
                LinkedTreeMap fragtol = (LinkedTreeMap) annotation.get("fragmentTolerance");
                ToleranceUnit tu = new ToleranceUnit((Double)fragtol.get("tolerance"), fragtol.get("unit").toString());
                // MS1 precursor inof
                Double expMZ = (Double) annotation.get("precursorMZ");
                Double calcMZ = (Double) annotation.get("calculatedMZ");
                Double charge = (Double) annotation.get("precursorCharge");
                Double diff = expMZ * charge - calcMZ * charge;
                boolean is_mis_mono = diff > 0.5;
                // is it matched as non-covalent peptide pairs
                Boolean noncovalent = (Boolean) annotation.get("noncovalent");
                //
                double  minCalcMZ = tu.getMinRange(calcMZ);
                double  maxCalcMZ = tu.getMaxRange(calcMZ);
                
                double  minExpMZ = tu.getMinRange(expMZ);
                double  maxExpMZ = tu.getMaxRange(expMZ);
                
                boolean expPeakFound = false;
                double expPeakIntensity = 0;
                boolean calcPeakFound = false;
                double calcPeakIntensity = 0;
                double maxIntensity = 0;                
                
                int dcount = 0;
                
                for (LinkedTreeMap peak : peaks) {
                    double mz = (Double) peak.get("mz");
                    double intens = (Double) peak.get("intensity");
                    if (maxIntensity < intens)
                        maxIntensity = intens;
                    if (mz >= minExpMZ && mz <= maxExpMZ) {
                        expPeakFound = true;
                        if (intens > expPeakIntensity)
                            expPeakIntensity = intens;
                    }
                    if (mz >= minCalcMZ && mz <= maxCalcMZ) {
                        calcPeakFound = true;
                        if (intens > calcPeakIntensity)
                            calcPeakIntensity = intens;
                    }
                }
                StringBuffer td = new StringBuffer("TT");
                boolean decoy = false;
                for (LinkedTreeMap pep : peptides) {
                    if ((Boolean)pep.get("isDecoy")) {
                        td.append("D");
                        dcount++;;
                    }
                }
                String std = td.substring(td.length()-2);
                
                System.out.print(psmid + ", ");
                System.out.print(score + ", ");
                System.out.print(rawfile + ", ");
                System.out.print(scan + ", ");
                System.out.print(hasPP + ", ");
                System.out.print(expMZ + ", " + expPeakFound + ", " + expPeakIntensity + ", " + (expPeakIntensity/maxIntensity) + ", " );
                System.out.print(calcMZ + ", " + calcPeakFound + ", " + calcPeakIntensity + ", " + (calcPeakIntensity/maxIntensity) + ", " );
                System.out.print((dcount>0 ? "decoy" : "") + ", " + std + ", " + dcount);
                System.out.print(", " + noncovalent);
                System.out.print(", " + ((noncovalent && expPeakFound) ? std : ""));
                System.out.print(", " + ((noncovalent && calcPeakFound) ? std : ""));
                System.out.print(", " + (((!noncovalent) && expPeakFound) ? std : ""));
                System.out.print(", " + (((!noncovalent) && calcPeakFound) ? std : ""));
                
                System.out.print(", " + ((is_mis_mono && noncovalent && calcPeakFound) ? std : ""));
                System.out.print(", " + ((is_mis_mono && (!noncovalent) && calcPeakFound) ? std : ""));
                System.out.println();
            } catch (Exception e) {
                System.err.println("error:" + e);
                System.err.println("error:" + s);
                e.printStackTrace(System.err);
                System.exit(-1);
            }
        }
    }
    
}
