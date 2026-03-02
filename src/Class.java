/**
 * Represents a class in the university timetabling system
 */
public class Class {
    private final int id;
    private final int courseId;
    private final int lecturerId;
    private final String type;
    private final int duration;
    
    /*
     * Constructor
     */
    public Class(int id, int courseId, int lecturerId, String type, int duration) {
        this.id = id;
        this.courseId = courseId;
        this.lecturerId = lecturerId;
        this.type = type;
        this.duration = duration;
    }
    
    /*
     * Constructor without ID
     */
    public Class(int courseId, int lecturerId, String type, int duration) {
        this(0, courseId, lecturerId, type, duration);
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public int getLecturerId() { return lecturerId; }
    public String getType() { return type; }
    public int getDuration() { return duration; }
    
    /**
     * Get the maximum capacity for this class type
     */
    public int getMaxCapacity() {
        switch (type.trim().toLowerCase()) {
            case "lecture":
                return 300;
            case "tutorial":
                return 30;
            case "practical":
                return 20;
            default:
                return 0;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Class{id=%d, courseId=%d, lecturerId=%d, type='%s', duration=%d}",
                id, courseId, lecturerId, type, duration);
    }
}
