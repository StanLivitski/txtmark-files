/**
 *  Copyright 2013 Konstantin Livitski
 *
 *  Konstantin Livitski licenses this file to You under the
 *  Apache License, Version 2.0  (the "License"); you may not use this file
 *  except in compliance with  the License.  You may obtain a copy of
 *  the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package name.livitski.tools.txtmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.github.rjeschke.txtmark.Processor;

/**
 * Converts multiple <code>.md</code> files into HTML files.
 * 
 * Syntax: <code><pre>
 * 	name.livitski.tools.txtmark.ConvertFiles { target-dir } { md-file ... }
 * 
 * Set the <code>name.livitski.tools.txtmark.encoding</code> system property to
 * change the encoding in which the files are read and written. If not set,
 * the system default encoding is used.
 * <br />
 * Set the <code>debug</code> system property to <code>true</code> to see the
 * complete stack trace of an error.
 */
public class ConvertFiles implements Runnable
{
 public static final String ENCODING_PROPERTY = "name.livitski.tools.txtmark.encoding";
 public static final String OVERWRITE_PROPERTY = "name.livitski.tools.txtmark.overwrite";
 public static final String RESUME_PROPERTY = "name.livitski.tools.txtmark.resume";

 public static void main(String[] args)
 {
  ConvertFiles tool = new ConvertFiles().withArgs(args);
  if (0 == tool.getStatus())
   tool.run();
  if (0 != tool.getStatus())
   System.exit(tool.getStatus());
 }

 public static String defaultEncoding()
 {
  String encoding = System.getProperty(ENCODING_PROPERTY);
  if (null == encoding)
  {
   Charset defaultCharset = Charset.defaultCharset();
   encoding = defaultCharset.name();
  }
  return encoding;
 }

 public ConvertFiles withArgs(String[] args)
 {
  if (0 == args.length)
  {
   System.err.println("Destination directory is a required argument.");
   status = 1;
   return this;
  }
  File destDir = new File(args[0]);
  if (!destDir.isDirectory())
  {
   System.err.println("There is no directory at \"" + destDir + '"');
   status = 2;
   return this;
  }
  this.destDir = destDir;
  this.inputEncoding = this.outputEncoding = defaultEncoding();
  if (1 == args.length)
  {
   System.err.println("Please specify at least one file to convert.");
   status = 3;
   return this;
  }
  files = new ArrayList<File>(args.length - 1);
  for (int i = 1; args.length > i; i++)
  {
   File file = new File(args[i]);
   if (!file.exists() || file.isDirectory())
   {
    System.err.println("There is no file at \"" + file + '"');
    status = 4;
    return this;
   }
   files.add(file);
  }
  return this;
 }

 public void run()
 {
  for (File file : files)
  {
   System.out.println("Converting file \"" + file + "\" ...");
   try
   {
    convert(file);
   }
   catch(Exception ex)
   {
    System.err.println("Error converting file \"" + file + "\":");
    if (Boolean.getBoolean("debug"))
     ex.printStackTrace();
    else
     System.err.println(ex.getMessage());
    status = 5;
    if (!Boolean.getBoolean(RESUME_PROPERTY))
     break;
   }
  }
 }

 public void convert(File file) throws Exception
 {
  if (file.isAbsolute())
   throw new IllegalArgumentException("Paths to converted files must be relative, got \"" + file + '"');
  String path = file.getPath();
  if (path.toLowerCase().endsWith(".md"))
   path = path.substring(0, path.length() - 3);
  path += ".html";
  File dest = new File(destDir, path);
  if (dest.exists() && !Boolean.getBoolean(OVERWRITE_PROPERTY))
   throw new IllegalStateException("Destination file \"" + dest + "\" already exists");
  File parent = dest.getCanonicalFile().getParentFile();
  parent.mkdirs();
  String html = Processor.process(file, inputEncoding);
  OutputStream ostr = null;
  try
  {
   ostr = new FileOutputStream(dest);
   PrintStream out = new PrintStream(ostr, false, outputEncoding);
   out.println("<!DOCTYPE html>");
   out.println("<html>");
   out.println("<head>");
   out.println("<meta charset=\"" + outputEncoding + "\" />");
   out.println("</head>");
   out.println("<body>");
   out.println(html);
   out.println("</body>");
   out.println("</html>");
   out.flush();
   if (out.checkError())
    throw new IOException("Error writing converted data");
  }
  finally
  {
   if (null != ostr)
    ostr.close();
  }
 }

 public int getStatus()
 {
  return status;
 }

 private File destDir;
 private List<File> files;
 private int status;
 private String inputEncoding, outputEncoding;
}
