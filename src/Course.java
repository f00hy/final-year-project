/**
 * Represents a course in the university timetabling system
 */
public class Course {
    private final int id;
    private final String code;
    private final String name;
    private final int year;
    private final int trimester;
    
    /*
     * Constructor
     */
    public Course(int id, String code, String name, int year, int trimester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.year = year;
        this.trimester = trimester;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getYear() { return year; }
    public int getTrimester() { return trimester; }
    
    @Override
    public String toString() {
        return String.format("Course{id=%d, code='%s', name='%s', year=%d, trimester=%d}",
                id, code, name, year, trimester);
    }
}
