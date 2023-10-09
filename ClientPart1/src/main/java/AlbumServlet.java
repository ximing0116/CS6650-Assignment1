import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AlbumServlet", value = "/AlbumServlet")
public class AlbumServlet extends HttpServlet {

    static Map<Integer, Album> albumStore = new HashMap<>();

    static class Album {
        public byte[] image;
        public Profile profile;
    }

    public static class Profile {
        public String artist;
        public String title;
        public String year;

        // You can add a method here to parse the JSON and fill the attributes
        void parseFromJson(String jsonData) throws IOException {
            ObjectMapper mapper = new ObjectMapper(); // Jackson's JSON processor
            Profile parsedProfile = mapper.readValue(jsonData, Profile.class);
            this.artist = parsedProfile.artist;
            this.year = parsedProfile.year;
            this.title = parsedProfile.title;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Extract album ID from the request URL parameters
            String pathInfo = request.getPathInfo();  // This should give "/7" based on your URL
            if (pathInfo == null || pathInfo.isEmpty()) {
                out.write(objectMapper.writeValueAsString(Map.of("error", "albumId path parameter is required")));
                return;
            }

            String[] split  = pathInfo.split("/");  // Removes the leading slash to get "7"
            String albumIdParam = split[split.length - 1];
            // System.out.println(pathInfo);
            // System.out.println(albumIdParam);

            // Parse the album ID
            int albumId = Integer.parseInt(albumIdParam);
            Album album = albumStore.get(albumId);

            // Check if album exists in the store
            if (album == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);  // 404 status code
                out.write(objectMapper.writeValueAsString(Map.of("error", "Album not found")));
                return;
            }

            // Serialize the Profile object instead of the whole Album
            String jsonResponse = objectMapper.writeValueAsString(album.profile);
            out.write(jsonResponse);


        } catch (NumberFormatException e) {
            out.write(objectMapper.writeValueAsString(Map.of("error", "Invalid albumId format")));
        } catch (Exception e) {
            out.write(objectMapper.writeValueAsString(Map.of("error", e.getMessage())));
        }
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            // Check if the request is multipart
            if (!ServletFileUpload.isMultipartContent(request)) {
                out.println("Error: Form must have enctype=multipart/form-data.");
                return;
            }

            // Parse the request to get file items.
            ServletFileUpload upload = new ServletFileUpload();

            // Parse the request to get the form data
            FileItemIterator iter = upload.getItemIterator(request);

            Album newAlbum = new Album();
            Profile profile = new Profile();

            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();

                if (!item.isFormField()) {
                    if ("image".equals(name)) {
                        newAlbum.image = IOUtils.toByteArray(stream);  // Apache Commons IO
                    }
                } else {
                    if ("profile".equals(name)) {
                        String profileJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
                        profile.parseFromJson(profileJson);
                    }
                }
            }

            newAlbum.profile = profile;

            // Add to the in-memory store and get the new album ID
            int newAlbumId = albumStore.size() + 1;
            albumStore.put(newAlbumId, newAlbum);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("albumId", newAlbumId);
            responseData.put("imageSize", newAlbum.image.length);

            // Convert the Map to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(responseData);

            // Set the response type to JSON and write the response
            out.write(jsonResponse);

        } catch (Exception ex) {
            out.println("File Upload Error: " + ex.getMessage());
        }
    }
}
