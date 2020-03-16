package ru.ifmo.rain.varfolomeev.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {
    private static final String EMPTY = "";
    private static final String FULL_NAME_FORMAT = "%s %s";
    private static final Comparator<Student> COMPARATOR_BY_NAME = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparingInt(Student::getId);

    private <E, T> E getGroup(Collection<Student> students,
                              Collector<Student, ?, T> groupCollector,
                              Function<Stream<Map.Entry<String, T>>, E> finisher) {
        return students.stream().collect(
                Collectors.collectingAndThen(Collectors.groupingBy(Student::getGroup, TreeMap::new, groupCollector),
                        t -> finisher.apply(t.entrySet().stream())));
    }

    private List<Group> getGroupBy(Collection<Student> students, Consumer<Map.Entry<String, List<Student>>> action) {
        return getGroup(students, Collectors.toList(),
                s -> s.peek(action).map(e -> new Group(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, e -> e.getValue().sort(COMPARATOR_BY_NAME));
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, e -> e.getValue().sort(Comparator.comparing(Student::getId)));
    }

    private String getLargestGroup(Collection<Student> students, Collector<Student, ?, Long> groupCollector) {
        return getGroup(students, groupCollector,
                s -> s.max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.<String, Long>comparingByKey().reversed()))
                        .map(Map.Entry::getKey).orElse(EMPTY));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroup(students, Collectors.counting());
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroup(students, Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(Student::getFirstName))), s -> (long) s.size()));
    }

    private Stream<String> mapped(List<Student> students, Function<Student, String> mapper) {
        return students.stream().map(mapper);
    }

    private List<String> get(List<Student> students, Function<Student, String> mapper) {
        return mapped(students, mapper).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return get(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return get(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return get(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return get(students, student -> String.format(FULL_NAME_FORMAT, student.getFirstName(), student.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapped(students, Student::getFirstName)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparingInt(Student::getId)).map(Student::getFirstName).orElse(EMPTY);
    }

    public List<Student> sortBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortBy(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortBy(students, COMPARATOR_BY_NAME);
    }

    private Stream<Student> filtered(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate);
    }

    private List<Student> findBy(Collection<Student> students, Predicate<Student> predicate) {
        return filtered(students, predicate).sorted(COMPARATOR_BY_NAME).collect(Collectors.toList());
    }

    private Predicate<Student> toPredicate(String key, Function<Student, String> mapper) {
        return student -> Objects.equals(key, mapper.apply(student));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findBy(students, toPredicate(name, Student::getFirstName));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findBy(students, toPredicate(name, Student::getLastName));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findBy(students, toPredicate(group, Student::getGroup));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filtered(students, toPredicate(group, Student::getGroup))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        (n1, n2) -> n1.compareTo(n2) < 0 ? n1 : n2)
                );
    }
}
