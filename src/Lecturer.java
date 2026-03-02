/**
 * Represents a lecturer in the university timetabling system
 */
public class Lecturer {
    private final int id;
    private final String name;
    private final int assignedClassHours;
    
    /*
     * Constructor
     */
    public Lecturer(int id, String name, int assignedClassHours) {
        this.id = id;
        this.name = name;
        this.assignedClassHours = assignedClassHours;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAssignedClassHours() { return assignedClassHours; }
    
    @Override
    public String toString() {
        return String.format("Lecturer{id=%d, name='%s', assignedClassHours=%d}",
                id, name, assignedClassHours);
    }
}
