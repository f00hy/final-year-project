import java.sql.*;
import java.util.*;

/**
 * Manages database connections and provides data access methods
 */
public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/university_timetabling";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    
    private Connection connection;
    
    /*
     * Constructor
     */
    public DatabaseManager() throws SQLException {
        try {
            java.lang.Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
    }
    
    /*
     * Get connection
     */
    public Connection getConnection() {
        return connection;
    }
    
    /*
     * Close connection
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    /**
     * ================================================
     * Course-related methods
     * ================================================
     */
    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM courses ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Course course = new Course(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getInt("year"),
                    rs.getInt("trimester")
                );
                courses.add(course);
            }
        }
        return courses;
    }
    
    /**
     * ================================================
     * Lecturer-related methods
     * ================================================
     */
    public List<Lecturer> getAllLecturers() throws SQLException {
        List<Lecturer> lecturers = new ArrayList<>();
        String query = "SELECT * FROM lecturers ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Lecturer lecturer = new Lecturer(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("assigned_class_hours")
                );
                lecturers.add(lecturer);
            }
        }
        return lecturers;
    }
    
    public List<Lecturer> getLecturersByCourse(int courseId) throws SQLException {
        List<Lecturer> lecturers = new ArrayList<>();
        String query = "SELECT l.* FROM lecturers l " +
                      "JOIN courses_lecturers cl ON l.id = cl.lecturer_id " +
                      "WHERE cl.course_id = ? ORDER BY l.assigned_class_hours ASC";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, courseId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Lecturer lecturer = new Lecturer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("assigned_class_hours")
                    );
                    lecturers.add(lecturer);
                }
            }
        }
        return lecturers;
    }
    
    public void updateLecturerAssignedHours(int lecturerId, int additionalHours) throws SQLException {
        String query = "UPDATE lecturers SET assigned_class_hours = assigned_class_hours + ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, additionalHours);
            stmt.setInt(2, lecturerId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * ================================================
     * Room-related methods
     * ================================================
     */
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT * FROM rooms ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("building").charAt(0),
                    rs.getString("type"),
                    rs.getInt("capacity")
                );
                rooms.add(room);
            }
        }
        return rooms;
    }
    
    /**
     * ================================================
     * Class-related methods
     * ================================================
     */
    public int insertClass(Class classObj) throws SQLException {
        String query = "INSERT INTO classes (course_id, lecturer_id, type, duration) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, classObj.getCourseId());
            stmt.setInt(2, classObj.getLecturerId());
            stmt.setString(3, classObj.getType());
            stmt.setInt(4, classObj.getDuration());
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating class failed, no ID obtained.");
                }
            }
        }
    }
    
    public List<Class> getAllClasses() throws SQLException {
        List<Class> classes = new ArrayList<>();
        String query = "SELECT * FROM classes ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Class classObj = new Class(
                    rs.getInt("id"),
                    rs.getInt("course_id"),
                    rs.getInt("lecturer_id"),
                    rs.getString("type"),
                    rs.getInt("duration")
                );
                classes.add(classObj);
            }
        }
        return classes;
    }
    
    /**
     * ================================================
     * Group-related methods
     * ================================================
     */
    public List<StudentGroup> getAllStudentGroups() throws SQLException {
        List<StudentGroup> groups = new ArrayList<>();
        String query = "SELECT * FROM student_groups ORDER BY id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                StudentGroup group = new StudentGroup(
                    rs.getInt("id"),
                    rs.getInt("student_count"),
                    rs.getInt("year"),
                    rs.getInt("trimester")
                );
                groups.add(group);
            }
        }
        return groups;
    }
    
    public void insertGroupClassAssignment(int groupId, int classId) throws SQLException {
        String query = "INSERT INTO groups_classes (group_id, class_id) VALUES (?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
        }
    }

    public List<Integer> getGroupsByClass(int classId) throws SQLException {
        List<Integer> groupIds = new ArrayList<>();
        String query = "SELECT group_id FROM groups_classes WHERE class_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, classId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    groupIds.add(rs.getInt("group_id"));
                }
            }
        }
        return groupIds;
    }

    public Map<Integer, Integer> getGroupCountsForAllClasses() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String query = "SELECT class_id, COUNT(*) AS cnt FROM groups_classes GROUP BY class_id";
        
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                counts.put(rs.getInt("class_id"), rs.getInt("cnt"));
            }
        }
        return counts;
    }
    
    /**
     * ================================================
     * Student-related methods
     * ================================================
     */
    public List<Student> getStudentsByGroup(int groupId) throws SQLException {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM students WHERE group_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Student student = new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("group_id")
                    );
                    students.add(student);
                }
            }
        }
        return students;
    }
    
    /**
     * ================================================
     * Detail-related methods
     * ================================================
     */
    public ClassDetail getClassDetail(int classId) throws SQLException {
        String query = "SELECT c.*, co.code as course_code, co.name as course_name, " +
                      "l.name as lecturer_name FROM classes c " +
                      "JOIN courses co ON c.course_id = co.id " +
                      "JOIN lecturers l ON c.lecturer_id = l.id " +
                      "WHERE c.id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, classId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    return new ClassDetail(
                        rs.getInt("id"),
                        rs.getInt("course_id"),
                        rs.getInt("lecturer_id"),
                        rs.getString("type"),
                        rs.getInt("duration"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("lecturer_name")
                    );
                }
            }
        }
        return null;
    }
}
