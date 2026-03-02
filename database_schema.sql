-- University timetabling system database schema
-- Drop database if exists
DROP DATABASE IF EXISTS university_timetabling;
CREATE DATABASE university_timetabling;
USE university_timetabling;

-- Student groups table
CREATE TABLE student_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_count TINYINT NOT NULL CHECK (student_count BETWEEN 1 AND 10),
    year TINYINT NOT NULL CHECK (year >= 1),
    trimester TINYINT NOT NULL CHECK (trimester BETWEEN 1 AND 3)
);

-- Students table
CREATE TABLE students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    group_id INT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE CASCADE
);

-- Courses table
CREATE TABLE courses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    year TINYINT NOT NULL CHECK (year >= 1),
    trimester TINYINT NOT NULL CHECK (trimester BETWEEN 1 AND 3)
);

-- Lecturers table
CREATE TABLE lecturers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    assigned_class_hours SMALLINT DEFAULT 0 CHECK (assigned_class_hours >= 0)
);

-- Courses-Lecturers relationship table
CREATE TABLE courses_lecturers (
    course_id INT,
    lecturer_id INT,
    PRIMARY KEY (course_id, lecturer_id),
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (lecturer_id) REFERENCES lecturers(id) ON DELETE CASCADE
);

-- Rooms table
CREATE TABLE rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    building CHAR(1) NOT NULL CHECK (building BETWEEN 'A' AND 'Z'),
    type ENUM('lecture hall', 'tutorial room', 'practical lab') NOT NULL,
    capacity SMALLINT NOT NULL CHECK (capacity > 0)
);

-- Classes table
CREATE TABLE classes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_id INT NOT NULL,
    lecturer_id INT NOT NULL,
    type ENUM('lecture', 'tutorial', 'practical') NOT NULL,
    duration TINYINT NOT NULL CHECK (duration BETWEEN 1 AND 10),
    FOREIGN KEY (course_id, lecturer_id) REFERENCES courses_lecturers(course_id, lecturer_id) ON DELETE CASCADE
);

-- Groups-Classes relationship table
CREATE TABLE groups_classes (
    group_id INT,
    class_id INT,
    PRIMARY KEY (group_id, class_id),
    FOREIGN KEY (group_id) REFERENCES student_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- Trigger to enforce class capacity constraint
DELIMITER //
CREATE TRIGGER check_class_capacity 
    BEFORE INSERT ON groups_classes 
    FOR EACH ROW 
BEGIN
    DECLARE total_students INT;
    DECLARE room_capacity INT;
    DECLARE class_type VARCHAR(20); 

    -- Get the total students that would be in the class
    SELECT COALESCE(SUM(sg.student_count), 0) + 
            (SELECT student_count FROM student_groups WHERE id = NEW.group_id)
    INTO total_students
    FROM groups_classes gc
    JOIN student_groups sg ON gc.group_id = sg.id
    WHERE gc.class_id = NEW.class_id;

    -- Get the class type to determine room capacity
    SELECT c.type
    INTO class_type
    FROM classes c
    WHERE c.id = NEW.class_id;

    -- Set capacity based on class type
    SET room_capacity = CASE
        WHEN class_type = 'lecture' THEN 300
        WHEN class_type = 'tutorial' THEN 30
        WHEN class_type = 'practical' THEN 20
        ELSE 0
    END;

    -- Check if capacity would be exceeded
    IF total_students > room_capacity
        THEN SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Class capacity would be exceeded'; 
    END IF;
END// 
DELIMITER ;
