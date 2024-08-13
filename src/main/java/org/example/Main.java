package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Solution solution = new Solution();
        String path = args[0];
        solution.rewriteFile(path);
    }

    static class Solution {

        private final List<Map<Long, Integer>> valuesInColumns = new ArrayList<>();
        private final List<String> valuesInLine = new ArrayList<>();
        private final Map<Integer, Set<String>> groupLinesMap = new HashMap<>();
        private List<GroupInfo> groups;
        // 2 variant
//        private final Map<Integer, Integer> lineGroupMap = new HashMap<>();
        private int globalGroup = 0;
        private String filePath;
        private final Set<String> emptyLines = new HashSet<>();

        void rewriteFile(String filePath) {

            this.filePath = filePath;
            long previousTime = System.currentTimeMillis();

            Path path = Paths.get(filePath);
            processFile(path);

//            Runtime r=Runtime.getRuntime();
            // 2 variant
//            fillLinesFromFiles(path);

            groups = new ArrayList<>(groupLinesMap.size());
            groupLinesMap.forEach((k, v) -> groups.add(new GroupInfo(k, v)));
            groups.sort((o1, o2) -> {
                if (o2.lines.size() == o1.lines.size()){
                    return o1.group - o2.group;
                }
                return o2.lines.size() - o1.lines.size();
            });

            rewriteChangedLinesToCurrentFile();
//            createNewFile();

            double timeSeconds = ((double) System.currentTimeMillis() - previousTime) / 1000;

//            System.out.println("Memory Used="+(r.totalMemory()-r.freeMemory()));
//            long atLeastTwoLines = groups.stream().filter(groupInfo -> groupInfo.lines.size() > 1).count();
//            System.out.println(atLeastTwoLines);
            System.out.println(timeSeconds);

        }

//        private void createNewFile() {
//
//            String fileExtension = Optional.ofNullable(filePath)
//                    .filter(f -> f.contains("."))
//                    .map(f -> f.substring(filePath.lastIndexOf(".") + 1)).orElse("");
//            File file = new File(filePath);
//
//            String newName = file.getName() + "processed"
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));) {
//
//                for (LineInfo lineInfo: lines) {
//                    writer.newLine();
//                }
//            } catch (Exception ignored) {
//                System.out.println(ignored.getMessage());
//            }
//
//        }

        private void rewriteChangedLinesToCurrentFile() {

            long atLeastTwoLines = groups.stream().filter(groupInfo -> groupInfo.lines.size() > 1).count();
            System.out.println(atLeastTwoLines);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("Number of groups with more than one element: " + atLeastTwoLines);

                for (GroupInfo element : groups) {

                    writer.newLine();
                    writer.newLine();
                    writer.write("Group: " + element.group);

                    for (String line: element.lines) {
                        writer.newLine();
                        writer.write(line);
                    }
                }

                for (String line: emptyLines) {
                    writer.newLine();
                    writer.newLine();
                    writer.write("Group: " + ++globalGroup);
                    writer.newLine();
                    writer.write(line);
                }

            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
        }

        private void processFile(Path path) {

            int numberOfLine = 0;
            try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    processLine(line, numberOfLine++);
                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }

            valuesInColumns.clear();

        }

        // 2 variant
//        private void fillLinesFromFiles(Path path) {
//
//            int numberOfLine = 0;
//            try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
//                String line;
//                while ((line = bufferedReader.readLine()) != null) {
//                    Integer group = lineGroupMap.get(numberOfLine++);
//                    if (group == null)
//                        continue;
//                    Set<String> lines = groupLinesMap.getOrDefault(group, new HashSet<>());
//                    lines.add(line);
//                    groupLinesMap.putIfAbsent(group, lines);
//                }
//            } catch (Exception exception) {
//                System.out.println(exception.getMessage());
//            }
//
//            lineGroupMap.clear();
//
//        }

        private void processLine(String line, int numberOfLine) {

            int column = 0;
            boolean isValid = true;
            int group = 0;
            int indexLast = 0;
            int index = line.indexOf(";");
            boolean lineWithOnlyEmptyColumns = true;
            while (index != -1 && index >= indexLast) {
                String value = line.substring(indexLast, index);
                indexLast = index + 1;
                index = line.indexOf(";", indexLast);
                if (index == -1) {
                    index = line.length();
                }

                if (isEmptyValue(value)) {
                    column++;
                    valuesInLine.add(value);
                    continue;
                }

                if (value.indexOf("\"", 1) < value.length() - 1) {
                    isValid = false;
                    break;
                }

                value = getCorrectedValue(value);

                if (group == 0) {

                    addColumnsIntoValuesOfColumns(column);
                    Map<Long, Integer> map = valuesInColumns.get(column);
                    int groupMap = map.getOrDefault(Long.parseLong(value), 0);
                    if (groupMap != 0) {
                        group = groupMap;
                    }

                }

                column++;
                valuesInLine.add(value);
                lineWithOnlyEmptyColumns = false;

            }

            if (!isValid) {
                valuesInLine.clear();
                return;
            }

            if (group == 0 && !lineWithOnlyEmptyColumns) {
                group = ++globalGroup;
            }

            fillValuesOfColumns(group);
            valuesInLine.clear();
            fillLinesAndEmptyLines(line, group, lineWithOnlyEmptyColumns, numberOfLine);

        }

        private void fillLinesAndEmptyLines(String line, int group, boolean lineWithOnlyEmptyColumns, int numberOfLine) {
            if (lineWithOnlyEmptyColumns) {
                emptyLines.add(line);
                return;
            }
            // 2 variant
//            lineGroupMap.putIfAbsent(numberOfLine, group);
            // current variant
            Set<String> lines = groupLinesMap.getOrDefault(group, new HashSet<>());
            lines.add(line);
            groupLinesMap.putIfAbsent(group, lines);
        }

        private String getCorrectedValue(String value) {

            int beginIndex = value.indexOf('\"') + 1;
            int endIndex = value.lastIndexOf('\"');
            int indexDot = value.indexOf(".");
            if (indexDot != -1) {
                endIndex = indexDot;
//                int indexOfLastChar = value.length() - 1;
//
//                while (value.charAt(indexOfLastChar) == '0') {
//                    indexOfLastChar--;
//                }
//                if (value.charAt(indexOfLastChar) == '.')
//                    indexOfLastChar--;
//                value = value.substring(0, indexOfLastChar + 1);
            }
            value = value.substring(beginIndex, endIndex);
            return value;
        }

        private boolean isEmptyValue(String value) {
            return value.isEmpty() || value.equals("\"\"");
        }

        private void fillValuesOfColumns(int group) {
            int column = 0;
            for (String value : valuesInLine) {
                if (isEmptyValue(value)) {
                    column++;
                    continue;
                }

                addColumnsIntoValuesOfColumns(column);
                Map<Long, Integer> map = valuesInColumns.get(column);
                map.putIfAbsent(Long.parseLong(value), group);

                column++;
            }
        }

        private void addColumnsIntoValuesOfColumns(int column) {
            if (valuesInColumns.size() <= column) {
                for (int i = valuesInColumns.size(); i <= column; i++) {
                    valuesInColumns.add(new HashMap<>());
                }
            }
        }

    }

    private record GroupInfo(int group, Set<String> lines) {
    }

}