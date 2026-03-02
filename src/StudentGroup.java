/**
 * Represents a student group in the university timetabling system
 */
public class StudentGroup {
    private final int id;
    private final int studentCount;
    private final int year;
    private final int trimester;
    
    /*
     * Constructor
     */
    public StudentGroup(int id, int studentCount, int year, int trimester) {
        this.id = id;
        this.studentCount = studentCount;
        this.year = year;
        this.trimester = trimester;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public int getStudentCount() { return studentCount; }
    public int getYear() { return year; }
    public int getTrimester() { return trimester; }
    
    @Override
    public String toString() {
        return String.format("StudentGroup{id=%d, studentCount=%d, year=%d, trimester=%d}",
                id, studentCount, year, trimester);
    }
}
