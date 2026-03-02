/**
 * Represents a room in the university timetabling system
 */
public class Room {
    private final int id;
    private final String name;
    private final char building;
    private final String type;
    private final int capacity;
    
    /*
     * Constructor
     */
    public Room(int id, String name, char building, String type, int capacity) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.type = type;
        this.capacity = capacity;
    }
    
    /*
     * Getters
     */
    public int getId() { return id; }
    public String getName() { return name; }
    public char getBuilding() { return building; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    
    /**
     * Check if this room is compatible with a specific class type
     */
    public boolean isCompatibleWith(String classType) {
        switch (classType.trim().toLowerCase()) {
            case "lecture":
                return type.equals("lecture hall");
            case "tutorial":
                return type.equals("tutorial room");
            case "practical":
                return type.equals("practical lab");
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Room{id=%d, name='%s', building='%c', type='%s', capacity=%d}",
                id, name, building, type, capacity);
    }
}
