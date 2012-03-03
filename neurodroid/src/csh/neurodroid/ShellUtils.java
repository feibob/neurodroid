// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

package csh.neurodroid;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class ShellUtils {

    private static String join(String[] sa, String delimiter) {
        Collection<String> s = Arrays.asList(sa);
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
    
    private static String getChmod() throws IOException {
        String chmod = "/system/bin/chmod";
        if (!(new File(chmod)).exists()) {
            chmod = "/system/xbin/chmod";
            if (!(new File(chmod)).exists()) {
                throw new IOException("Couldn't find chmod on your system");
            }
        }
        return chmod;
    }
    
    public static void chmod(String filePath, String mod)
            throws IOException, InterruptedException
    {
        String[] chmodlist = {getChmod(), mod, filePath};
        ShellUtils.runBinary(chmodlist);
    }
    
    public static boolean supportsFuse() {
        return (new File("/dev/fuse")).exists();
    }
    
    public static String runBinary(String[] binName)
            throws IOException, InterruptedException
    {
        return runBinary(binName, "/", false, false, null, null);
    }

/*    public static String runBinary(String[] binName, String binDir, boolean stderr)
            throws IOException, InterruptedException
    {
        return runBinary(binName, binDir, stderr, false, null, null);
    }
*/
    /** Run a binary using binDir as the wd. Return stdout
     *  and optinally stderr
     * @throws IOException 
     * @throws InterruptedException 
     */
    public static String runBinary(String[] binName, String binDirPath, boolean stderr, boolean root,
            String toStdIn, String[] envPrepend)
            throws IOException, InterruptedException
    {
        File binDir = new File(binDirPath);
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
        
        String NL = System.getProperty("line.separator");
        ProcessBuilder pb = new ProcessBuilder(binName);
        pb.directory(binDir);
        Map<String, String> env = pb.environment();
        if (envPrepend != null && envPrepend.length > 1 && envPrepend.length % 2 ==0) {
            for (int nenv=0; nenv<envPrepend.length; nenv+=2) {
                env.put(envPrepend[nenv], envPrepend[nenv+1]);
            }
        }
        
        pb.redirectErrorStream(stderr);
        
        if (root) {
            String[] sucmd = {"su", "-c", join(binName, " ")};
            pb.command(sucmd);
        } else {
            pb.command(binName);
        }

        Process process = pb.start();
        
        if (toStdIn != null) {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()) );
            writer.write(toStdIn + "\n");
            writer.flush();
        }

        process.waitFor();
            
        String output = "";
        Scanner outscanner = new Scanner(new BufferedInputStream(process.getInputStream()));
        try {
            while (outscanner.hasNextLine()) {
                output += outscanner.nextLine();
                output += NL;
            }
        }
        finally {
            outscanner.close();
        }

        return output;

    }
    
    public static boolean isMounted(String mountName) {
        boolean isMounted = false;
        try {
            /* Read mounted info */
            FileInputStream fis = new FileInputStream("/proc/mounts");
            Scanner scanner = new Scanner(fis);
            try {
                while (scanner.hasNextLine() && !isMounted) {
                    if (!isMounted && scanner.findInLine(mountName)!=null) {
                        isMounted = true;
                    }
                    scanner.nextLine();
                }
            } finally {
                scanner.close();
            }
        } catch (IOException e) {
            return isMounted;
        }
        return isMounted;
    }

}
