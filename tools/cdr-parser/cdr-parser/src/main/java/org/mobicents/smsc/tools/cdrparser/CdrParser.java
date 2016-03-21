/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.tools.cdrparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.TreeMap;

import javolution.util.FastMap;

public class CdrParser {

    public static void main(String[] args) {
        CdrParser aaa = new CdrParser();
        aaa.load("E:\\support\\customers\\SMS_it\\08\\cdr.log", "E:\\support\\customers\\SMS_it\\08\\cdr.txt");
    }

    private void load(String fileName, String outName) {
        String dest = "Creval SMS";

        FastMap<String, ArrayList<Cdr>> al1 = new FastMap<String, ArrayList<Cdr>>();
        TreeMap<String, Cdr> sl2 = new TreeMap<String, Cdr>();

        try (BufferedReader fileOut = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {

            for (String line; (line = fileOut.readLine()) != null;) {
                String[] ss = line.split(",");
                if (ss.length >= 10) {
                    Cdr cdr = new Cdr();
                    cdr.a_from = ss[3];
                    cdr.a_to = ss[6];
                    cdr.cause = ss[9];
                    cdr.date = ss[0];
                    cdr.esme = ss[10];
                    if (ss.length >= 17)
                        cdr.reason = ss[16];

                    if (cdr.cause.equals("failed") || cdr.cause.equals("success") || cdr.cause.equals("partial")) {
                        String from_to = cdr.a_from + "_" + cdr.a_to;
                        ArrayList<Cdr> al = al1.get(from_to);
                        if (al == null) {
                            al = new ArrayList<Cdr>();
                            al1.put(from_to, al);
                        }
                        al.add(cdr);
                    } else if (cdr.cause.equals("success_esme")) {
                        String from_to = cdr.a_to + "_" + cdr.a_from;
                        ArrayList<Cdr> al = al1.get(from_to);
                        if (al != null && !al.isEmpty()) {
                            Cdr cdr2 = al.remove(0);
                            if (cdr2.a_from.equals(dest))
                                sl2.put(cdr2.date, cdr2);
                        } else {
                            int gg = 0;
                            gg++;
                        }
                    } else if (cdr.cause.equals("temp_failed") || cdr.cause.equals("temp_failed_esme")) {
                    } else {
                        int gg = 0;
                        gg++;
                    }
                }
            }

            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<String> toDel = new ArrayList<String>();
        for (String s : al1.keySet()) {
            ArrayList<Cdr> al = al1.get(s);
            if (al.isEmpty())
                toDel.add(s);
        }
        for (String s : toDel) {
            al1.remove(s);
        }

        try {
            BufferedWriter fileOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outName)));
            TreeMap<String, Cdr> sl = new TreeMap<String, Cdr>();
            for (String s : al1.keySet()) {
                ArrayList<Cdr> al = al1.get(s);
                for (Cdr cdr : al) {
                    if (cdr.a_from.equals(dest)) {
                        sl.put(cdr.date, cdr);
                    }
                }
            }

            for (Cdr cdr : sl.values()) {
                fileOut.write(cdr.toString());
                fileOut.newLine();
            }

            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            BufferedWriter fileOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outName + "2")));
            for (Cdr cdr : sl2.values()) {
                fileOut.write(cdr.toString());
                fileOut.newLine();
            }

            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int gg = 0;
        gg++;
    }

    public class Cdr {
        String a_from;
        String a_to;
        String cause;
        String date;
        String esme;
        String reason;

        public String toString() {
            return a_from + "->" + a_to + " " + date + " - " + esme + " - " + cause + " - " + reason;
        }
    }
}
