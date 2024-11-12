package edu.cs;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet({"/FileUploadServlet"})
@MultipartConfig(
        fileSizeThreshold = 10485760,
        maxFileSize = 52428800L,
        maxRequestSize = 104857600L
)
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 205242440643911308L;
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String applicationPath = request.getServletContext().getRealPath("");
        String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;
        File fileSaveDir = new File(uploadFilePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdirs();
        }

        System.out.println("Upload File Directory=" + fileSaveDir.getAbsolutePath());
        String fileName = "";
        for (Part part : request.getParts()) {
            fileName = getFileName(part);
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            part.write(uploadFilePath + File.separator + fileName);
        }

        String message = "Result";
        String content = "";
        File uploadedFile = new File(uploadFilePath + File.separator + fileName);
        if (uploadedFile.exists() && uploadedFile.length() > 0) {
            if (fileName.endsWith(".txt")) {
                try (Scanner scanner = new Scanner(uploadedFile)) {
                    content = scanner.useDelimiter("\\Z").next();
                } catch (NoSuchElementException e) {
                    content = "File is empty.";
                }
            } else {
                content = "Binary file uploaded successfully.";
            }
        } else {
            content = "File is empty or not found.";
        }
        response.getWriter().write(message + "<BR>" + content);

        // Database connection details
        String dbUrl = "jdbc:mysql://localhost:3306/cybersecurity";
        String dbUser = "db_user";
        String dbPassword = "Frank1614";

        // Load JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.getWriter().write("Error loading JDBC Driver: " + e.getMessage());
            return;
        }

        // Insert file upload information into the database
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String insertSQL = "INSERT INTO uploaded_files (file_name, upload_time) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertSQL);
            statement.setString(1, fileName);
            statement.setObject(2, LocalDateTime.now());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().write("Error saving file info to database: " + e.getMessage());
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= " + contentDisp);
        String[] tokens = contentDisp.split(";");
        String[] var4 = tokens;
        int var5 = tokens.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String token = var4[var6];
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }

        return "";
    }

    private void writeToResponse(HttpServletResponse resp, String results) throws IOException {
        PrintWriter writer = new PrintWriter(resp.getOutputStream());
        resp.setContentType("text/plain");
        if (results.isEmpty()) {
            writer.write("No results found.");
        } else {
            writer.write(results);
        }

        writer.close();
    }
}
