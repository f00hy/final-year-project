/**
 * Represents a student in the university timetabling system
 */
public class Student {
    private final int id;
    private final String name;
    private final int groupId;
    
    /*
     * Constructor
     */
    public Student(int id, String name, int groupId) {
        this.id = id;
        this.name = name;
        this.groupId = groupId;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public String getName() { return name; }
    public int getGroupId() { return groupId; }
    
    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', groupId=%d}", id, name, groupId);
    }
}
