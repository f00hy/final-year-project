-- Data generation script for university timetabling system
USE university_timetabling;

-- Insert rooms data
INSERT INTO rooms (id, name, building, type, capacity) VALUES 
(1, 'L1', 'L', 'lecture hall', 300),
(2, 'N1', 'N', 'tutorial room', 30),
(3, 'N2', 'N', 'tutorial room', 30),
(4, 'N3', 'N', 'practical lab', 20),
(5, 'N4', 'N', 'practical lab', 20);

-- Insert courses data (from data.txt)
INSERT INTO courses (id, code, name, year, trimester) VALUES 
(1, 'UCCD1024', 'DATA STRUCTURE AND ALGORITHMIC PROBLEM SOLVING', 1, 3),
(2, 'UCCD1203', 'DATABASE DEVELOPMENT AND APPLICATIONS', 1, 3),
(3, 'UCCD2003', 'OBJECT-ORIENTED SYSTEMS ANALYSIS AND DESIGN', 1, 3),
(4, 'UCCM1353', 'BASIC ALGEBRA', 1, 3),
(5, 'UCCM1363', 'DISCRETE MATHEMATICS', 1, 3),
(6, 'MPU3152', 'PENGHAYATAN ETIKA DAN PERADABAN', 1, 3);

-- Insert lecturers data (from data.txt)
INSERT INTO lecturers (id, name, assigned_class_hours) VALUES 
(1, 'Ts Dr Goh Chuan Meng', 0),
(2, 'Ts Lai Siew Cheng', 0),
(3, 'Cik Norazira Binti A Jalil', 0),
(4, 'Cik Ana Nabilah Binti Sa''uadi', 0),
(5, 'Dr Altahir Abdalla Altahir Mohammed', 0),
(6, 'Puan Lyana Izzati Binti Mohd Asri', 0),
(7, 'Ts Saravanan a/l Subbiah', 0),
(8, 'Dr Zurida Binti Ishak', 0),
(9, 'Ts Dr Ku Chin Soon', 0),
(10, 'Ts Dr Mogana a/p Vadiveloo', 0),
(11, 'Dr Tahayna Bashar M. A.', 0),
(12, 'Cik Puteri Nursyawati Binti Azzuri', 0),
(13, 'Ms Lim Shun Jinn', 0),
(14, 'Dr Nur Amalina Binti Mat Jan', 0),
(15, 'Ms Song Poh Choo', 0),
(16, 'Puan Sarah Binti Shamshul Anwar', 0);

-- Insert courses-lecturers relationships (from data.txt)
INSERT INTO courses_lecturers (course_id, lecturer_id) VALUES 
-- UCCD1024
(1, 1), (1, 2),
-- UCCD1203
(2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8),
-- UCCD2003
(3, 9), (3, 10), (3, 11), (3, 12),
-- UCCM1353
(4, 13),
-- UCCM1363
(5, 14), (5, 15),
-- MPU3152
(6, 16);

-- Generate 15 student groups
INSERT INTO student_groups (id, student_count, year, trimester) VALUES 
(1, 10, 1, 3), (2, 10, 1, 3), (3, 10, 1, 3), (4, 10, 1, 3), (5, 10, 1, 3),
(6, 10, 1, 3), (7, 10, 1, 3), (8, 10, 1, 3), (9, 10, 1, 3), (10, 10, 1, 3),
(11, 10, 1, 3), (12, 10, 1, 3), (13, 10, 1, 3), (14, 10, 1, 3), (15, 10, 1, 3);

-- Generate 150 students
INSERT INTO students (name, group_id) VALUES
-- Group 1 students
('Student_001', 1), ('Student_002', 1), ('Student_003', 1), ('Student_004', 1), ('Student_005', 1),
('Student_006', 1), ('Student_007', 1), ('Student_008', 1), ('Student_009', 1), ('Student_010', 1),
-- Group 2 students
('Student_011', 2), ('Student_012', 2), ('Student_013', 2), ('Student_014', 2), ('Student_015', 2),
('Student_016', 2), ('Student_017', 2), ('Student_018', 2), ('Student_019', 2), ('Student_020', 2),
-- Group 3 students
('Student_021', 3), ('Student_022', 3), ('Student_023', 3), ('Student_024', 3), ('Student_025', 3),
('Student_026', 3), ('Student_027', 3), ('Student_028', 3), ('Student_029', 3), ('Student_030', 3),
-- Group 4 students
('Student_031', 4), ('Student_032', 4), ('Student_033', 4), ('Student_034', 4), ('Student_035', 4),
('Student_036', 4), ('Student_037', 4), ('Student_038', 4), ('Student_039', 4), ('Student_040', 4),
-- Group 5 students
('Student_041', 5), ('Student_042', 5), ('Student_043', 5), ('Student_044', 5), ('Student_045', 5),
('Student_046', 5), ('Student_047', 5), ('Student_048', 5), ('Student_049', 5), ('Student_050', 5),
-- Group 6 students
('Student_051', 6), ('Student_052', 6), ('Student_053', 6), ('Student_054', 6), ('Student_055', 6),
('Student_056', 6), ('Student_057', 6), ('Student_058', 6), ('Student_059', 6), ('Student_060', 6),
-- Group 7 students
('Student_061', 7), ('Student_062', 7), ('Student_063', 7), ('Student_064', 7), ('Student_065', 7),
('Student_066', 7), ('Student_067', 7), ('Student_068', 7), ('Student_069', 7), ('Student_070', 7),
-- Group 8 students
('Student_071', 8), ('Student_072', 8), ('Student_073', 8), ('Student_074', 8), ('Student_075', 8),
('Student_076', 8), ('Student_077', 8), ('Student_078', 8), ('Student_079', 8), ('Student_080', 8),
-- Group 9 students
('Student_081', 9), ('Student_082', 9), ('Student_083', 9), ('Student_084', 9), ('Student_085', 9),
('Student_086', 9), ('Student_087', 9), ('Student_088', 9), ('Student_089', 9), ('Student_090', 9),
-- Group 10 students
('Student_091', 10), ('Student_092', 10), ('Student_093', 10), ('Student_094', 10), ('Student_095', 10),
('Student_096', 10), ('Student_097', 10), ('Student_098', 10), ('Student_099', 10), ('Student_100', 10),
-- Group 11 students
('Student_101', 11), ('Student_102', 11), ('Student_103', 11), ('Student_104', 11), ('Student_105', 11),
('Student_106', 11), ('Student_107', 11), ('Student_108', 11), ('Student_109', 11), ('Student_110', 11),
-- Group 12 students
('Student_111', 12), ('Student_112', 12), ('Student_113', 12), ('Student_114', 12), ('Student_115', 12),
('Student_116', 12), ('Student_117', 12), ('Student_118', 12), ('Student_119', 12), ('Student_120', 12),
-- Group 13 students
('Student_121', 13), ('Student_122', 13), ('Student_123', 13), ('Student_124', 13), ('Student_125', 13),
('Student_126', 13), ('Student_127', 13), ('Student_128', 13), ('Student_129', 13), ('Student_130', 13),
-- Group 14 students
('Student_131', 14), ('Student_132', 14), ('Student_133', 14), ('Student_134', 14), ('Student_135', 14),
('Student_136', 14), ('Student_137', 14), ('Student_138', 14), ('Student_139', 14), ('Student_140', 14),
-- Group 15 students
('Student_141', 15), ('Student_142', 15), ('Student_143', 15), ('Student_144', 15), ('Student_145', 15),
('Student_146', 15), ('Student_147', 15), ('Student_148', 15), ('Student_149', 15), ('Student_150', 15);
