package com.unischeduler;

public class Course {
    public final String id;
    public final String name;
    public final Professor professor;
    public final int students;

    public Course(String id, String name, Professor professor, int students) {
        this.id = id;
        this.name = name;
        this.professor = professor;
        this.students = students;
    }
}
