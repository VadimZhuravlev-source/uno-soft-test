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
        solution.createFile(path);
    }

    static class Solution {

        private final List<LineInfo> lines = new ArrayList<>();
        private final List<Map<String, Integer>> valuesInColumns = new ArrayList<>();
        private final List<String> valuesInLine = new ArrayList<>();
        private int globalGroup = 0;
        private String filePath;
        private final Set<String> emptyLines = new HashSet<>();

        void createFile(String filePath) {

            this.filePath = filePath;
            long previousTime = System.currentTimeMillis();

            Path path = Paths.get(filePath);
            processFile(path);

            lines.sort(this::sortByGroup);
            removeDuplicates();
            fillNumberOfElementsInGroup();
            lines.sort(this::sortByNumberOfElements);
            addEmptyLines();

            rewriteChangedLinesToCurrentFile();
//            createNewFile();

            long timeSeconds = (System.currentTimeMillis() - previousTime) / 1000;
//            Runtime r=Runtime.getRuntime();
//            System.out.println("Memory Used="+(r.totalMemory()-r.freeMemory()));
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

            long atLeastTwoLines = getAtLeastTwoElements();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("Number of groups with more than one element: " + atLeastTwoLines);
                writer.newLine();

                int previousGroup = -1;
                for (LineInfo element : lines) {
                    if (previousGroup != element.group) {
                        writer.newLine();
                        writer.write("Group: " + element.group);
                        writer.newLine();
                    }
                    writer.write(element.line);
                    writer.newLine();
                    previousGroup = element.group;
                }

            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
        }

        private void processFile(Path path) {

            try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    processLine(line);
                }
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }

        }

        private void processLine(String line) {

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

                if (value.isEmpty() || value.equals("\"\"")) {
                    column++;
                    valuesInLine.add(value);
                    continue;
                }

                if (value.indexOf("\"", 1) < value.length() - 1) {
                    isValid = false;
                    break;
                }

                if (group == 0) {

                    addColumnsIntoValuesOfColumns(column);
                    Map<String, Integer> map = valuesInColumns.get(column);
                    int groupMap = map.getOrDefault(value, 0);
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

            if (group == 0) {
                group = ++globalGroup;
            }

            fillValuesOfColumns(group);
            valuesInLine.clear();
            fillLinesAndEmptyLines(line, group, lineWithOnlyEmptyColumns);

        }

        private void fillLinesAndEmptyLines(String line, int group, boolean lineWithOnlyEmptyColumns) {
            if (lineWithOnlyEmptyColumns) {
                emptyLines.add(line);
                return;
            }
            lines.add(new LineInfo(line, group));
        }

        private void fillValuesOfColumns(int group) {
            int column = 0;
            for (String value : valuesInLine) {
                if (value.isEmpty()) {
                    column++;
                    continue;
                }

                addColumnsIntoValuesOfColumns(column);
                Map<String, Integer> map = valuesInColumns.get(column);
                map.putIfAbsent(value, group);

                column++;
            }
        }

        private int sortByGroup(LineInfo o1, LineInfo o2) {

            if (o1.group == o2.group) {
                return o1.line.compareTo(o2.line);
            }
            return o1.group - o2.group;

        }

        private int sortByNumberOfElements(LineInfo o1, LineInfo o2) {

            if (o1.number == o2.number) {
                return o1.group - o2.group;
            }
            return o2.number - o1.number;

        }

        private int getAtLeastTwoElements() {
            int previousGroup = -1;
            List<Integer> groupElements = new ArrayList<>();
            for (LineInfo element : lines) {
                if (element.number <= 1) {
                    break;
                }
                if (previousGroup != element.group) {
                    groupElements.add(element.group);
                }
                previousGroup = element.group;
            }
            return groupElements.size();
        }

        private void removeDuplicates() {
            LineInfo previousElement = null;
            List<LineInfo> deletingElements = new ArrayList<>();
            for (LineInfo element : lines) {
                if (previousElement == null || previousElement.group != element.group) {
                    previousElement = element;
                    continue;
                }
                if (element.line.equals(previousElement.line)) {
                    deletingElements.add(element);
                }
                previousElement = element;
            }

            lines.removeAll(deletingElements);
        }

        private void fillNumberOfElementsInGroup() {
            int previousGroup = -1;
            List<LineInfo> groupElements = new ArrayList<>();
            for (LineInfo element : lines) {
                if (previousGroup != element.group) {
                    groupElements.forEach(elem -> elem.number = groupElements.size());
                    groupElements.clear();
                }
                groupElements.add(element);
                previousGroup = element.group;
            }
        }

        private void addEmptyLines() {
            for (String empty: emptyLines) {
                lines.add(new LineInfo(empty, ++globalGroup, 1));
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

    static class LineInfo {

        String line;
        int group;
        int number;

        public LineInfo(String line, int group) {
            this.line = line;
            this.group = group;
        }

        public LineInfo(String line, int group, int number) {
            this.line = line;
            this.group = group;
            this.number = number;
        }

    }

}