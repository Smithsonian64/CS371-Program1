/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format).
 *
 * Modified by Michael Smith
 * January 2020
*
**/

import java.net.InetAddress;
import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;
private String request;
File file;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os,"text/html");
      writeContent(os);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is) {

   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   try{
      request = r.readLine();
      System.err.println("Request Line: ("+request+")");
      Thread.sleep(2);
   }
   catch (Exception e) {
      System.err.println("Request error: "+e);

   }
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) {
            break;
         }
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
 * If request points to nonexistent html file or directory a 404 status code is written to the header
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   file = new File(parseRequest(request));
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if(file.isFile() || file.toString().equals("")) os.write("HTTP/1.1 200 OK\n".getBytes());
   else os.write("HTTP/1.1 404 Not Found\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Michael's very own server\n".getBytes());
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
 * This method also replaces the special tags {{cs371date}} and {{cs371server}} with their
 * respective information on the response html file. If the request points to a non existent file or directory then
 * a 404 page is served.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{

   parseRequest(request);

   String oldFile = "";
   String newFile;

   BufferedReader reader = new BufferedReader(new FileReader("TestBase.html"));

   String line = reader.readLine();

   while (line != null)
   {
      oldFile = oldFile + line + System.lineSeparator();
      line = reader.readLine();
   }

   Date date = Calendar.getInstance().getTime();
   DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

   newFile = oldFile.replaceAll("\\{\\{cs371date}}", dateFormat.format(date));

   newFile = newFile.replaceAll("\\{\\{cs371server}}", System.getProperty("user.name") + " on " + InetAddress.getLocalHost());

   FileWriter writer = new FileWriter("Test.html");

   writer.write(newFile);

   reader.close();
   writer.close();

   File notFound = new File("notFound.html");

   if(file.toString().equals("")) {
      Files.copy(new File("Test.html").toPath(), os);
      return;
   }

   if(file.isFile()) {
      try {
         Files.copy(file.toPath(), os);
         return;
      }
      catch (Exception e) {
         os.write("<html><head>Bad request</head></html>".getBytes());
         System.err.println(e);
         return;
      }
   } else {
      try {
         Files.copy(notFound.toPath(), os);
         return;
      }
      catch (Exception e) {
         Files.copy(notFound.toPath(), os);
         return;
      }
   }



}

   /**
    * This method parsed the request file name from the HTTP GET request in order to server the write html file
    * @param request HTTP GET request
    * @return file name String
    */
   private static String parseRequest(String request) {

   String parsedRequest = request.substring(request.indexOf('/') + 1);
   parsedRequest = parsedRequest.substring(0, parsedRequest.indexOf(' '));

   return parsedRequest;
}

} // end class
