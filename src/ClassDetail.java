/**
 * Represents detailed class information including course and lecturer details
 */
public class ClassDetail {
    private final int id;
    private final int courseId;
    private final int lecturerId;
    private final String type;
    private final int duration;
    private final String courseCode;
    private final String courseName;
    private final String lecturerName;
    
    /*
     * Constructor
     */
    public ClassDetail(int id, int courseId, int lecturerId, String type, int duration,
                      String courseCode, String courseName, String lecturerName) {
        this.id = id;
        this.courseId = courseId;
        this.lecturerId = lecturerId;
        this.type = type;
        this.duration = duration;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public int getLecturerId() { return lecturerId; }
    public String getType() { return type; }
    public int getDuration() { return duration; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getLecturerName() { return lecturerName; }
    
    /**
     * Get a short description of the class
     */
    public String getDescription() {
        return String.format("%s (%s)", courseCode, type.toUpperCase());
    }
    
    @Override
    public String toString() {
        return String.format("ClassDetail{id=%d, courseCode='%s', type='%s', duration=%d, lecturer='%s'}",
                id, courseCode, type, duration, lecturerName);
    }
}
