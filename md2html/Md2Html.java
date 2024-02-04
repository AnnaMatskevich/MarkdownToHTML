package md2html;

import java.util.*;
import java.io.*;

public class Md2Html {
    static Map<String, String> mdToHTML = Map.of(
            "*", "em",
            "**", "strong",
            "_", "em",
            "__", "strong",
            "--", "s",
            "`", "code",
            "{{", "del",
            "}}", "del",
            "<<", "ins",
            ">>", "ins"
    );
    static Map<String, String> special = Map.of(
            "<", "&lt;",
            ">", "&gt;",
            "&", "&amp;"
    );

    public static void setFalse(Map<String, Boolean> myMap) {
        for (String mdCharElement : mdToHTML.keySet()) {
            myMap.put(mdCharElement, false);
        }
    }

    public static String getType(String line, int pos) {
        for (String mdCharElement : mdToHTML.keySet()) {
            if (mdCharElement.length() == 2) {
                if ((pos + 1 < line.length()) && line.substring(pos, pos + 2).equals(mdCharElement)) {
                    return mdCharElement;
                }
            }
        }
        if (line.charAt(pos) == '\\' & pos + 1 < line.length()) {
            String t2 = getType(line, pos + 1);
            return line.substring(pos, pos + 1 + t2.length());

        }
        return String.valueOf(line.charAt(pos));
    }

    public static void classify(String t, Map<String, Boolean> met, ArrayList<Integer> mark_conv, Map<String, Integer> last_met) {
        if (Objects.equals(t, "}}")) {
            met.put("{{", true);
            last_met.put("{{", mark_conv.size() - 1);
        } else {
            if (Objects.equals(t, "<<")) {
                met.put(">>", true);
                last_met.put(">>", mark_conv.size() - 1);
            } else {
                met.put(t, true);
                last_met.put(t, mark_conv.size() - 1);
            }
        }
    }

    public static List<String> getParagraph(List<String> lines, String line, BufferedReader in) throws IOException {
        while (line != null && !line.isEmpty()) {
            lines.add(line);
            line = in.readLine();
        }
        return lines;

    }

    public static int skipUnclosed(int wait, ArrayList<Integer> mark_conv) {
        while (wait < mark_conv.size() && mark_conv.get(wait) != 1) {
            wait++;
        }
        return wait;
    }

    public static int skipOctothorpes(int pos, List<String> lines) {
        while (pos < lines.get(0).length() && lines.get(0).charAt(pos) == '#') {
            pos++;
        }
        if (!(pos < lines.get(0).length() && lines.get(0).charAt(pos) == ' ')) {
            pos = 0;
        }
        return pos;
    }

    public static void countStack(ArrayList<String> markOrd, List<String> lines, int pos, ArrayList<Integer> markConv, Map<String, Boolean> met, Map<String, Integer> lastMet) {
        for (int j = 0; j < lines.size(); ++j) {
            String line = lines.get(j);
            while (pos < line.length()) {
                String t = getType(line, pos);
                pos += t.length() - 1;
                if (!met.containsKey(t)) {
                    pos++;
                    continue;
                }
                if (met.get(t)) {
                    markConv.set(lastMet.get(t), 1);
                    met.put(t, false);
                } else {
                    markOrd.add(t);
                    markConv.add(0);
                    classify(t, met, markConv, lastMet);

                }
                pos++;
            }
            pos = 0;
        }
    }

    public static void escapeCharacter(BufferedWriter out, String t, int i) throws IOException {
        out.write(special.getOrDefault(String.valueOf(t.charAt(i)), String.valueOf(t.charAt(i))));
    }

    public static int parsedMD(Map<String, Integer> lastMet, ArrayList<String> markOrd, boolean needToFind, int wait, ArrayList<Integer> markConv, Map<String, Boolean> met, String t, BufferedWriter out, Map<String, String> mdToHTML) throws IOException {
        if (needToFind && t.equals(markOrd.get(wait))) {
            classify(t, met, markConv, lastMet);
            out.write("<");
            out.write(mdToHTML.get(t));
            out.write(">");
            wait++;
            wait = skipUnclosed(wait, markConv);

        } else {
            for (int i = 0; i < t.length(); ++i) {
                escapeCharacter(out, t, i);
            }

        }
        return wait;
    }


    public static void main(String[] args) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
            try {
                String line = in.readLine();
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
                    try {
                        while (line != null) {
                            // :NOTE: код, который, выделяет абзацы
                            List<String> lines = new ArrayList<>();
                            lines = getParagraph(lines, line, in);
                            int pos = 0;
                            if (lines.isEmpty()) { // skip empty lines
                                line = in.readLine();
                                continue;
                            }
                            pos = skipOctothorpes(pos, lines);

                            int titleLevel = pos;
                            if (pos > 0) {
                                out.write("<h");
                                out.write(Integer.toString(pos));
                                out.write(">");
                                pos++;

                            } else {
                                out.write("<p>");
                            }
                            ArrayList<String> markOrd = new ArrayList<>();
                            ArrayList<Integer> markConv = new ArrayList<>();
                            Map<String, Integer> lastMet = new HashMap<>();
                            Map<String, Boolean> met = new HashMap<>();
                            setFalse(met);
                            countStack(markOrd, lines, pos, markConv, met, lastMet);
                            setFalse(met); // met -> opened
                            int wait = 0;
                            System.out.println(markOrd);
                            System.out.println(markConv);
                            wait = skipUnclosed(wait, markConv);
                            boolean needToFind = wait < markConv.size();
                            for (int j = 0; j < lines.size(); ++j) {
                                line = lines.get(j);
                                while (pos < line.length()) {
                                    String t = getType(line, pos);
                                    pos += t.length() - 1;
                                    if (!met.containsKey(t)) {
                                        if (t.length() > 1) {
                                            // :NOTE: escapeCharacters
                                            for (int i = 1; i < t.length(); ++i) {
                                                escapeCharacter(out, t, i);
                                            }
                                            pos++;
                                            continue;
                                        }
                                        out.write(special.getOrDefault(t, t));
                                        pos++;
                                        continue;
                                    }
                                    if (met.get(t)) {
                                        met.put(t, false);
                                        out.write("</");
                                        out.write(mdToHTML.get(t));
                                        out.write(">");
                                        pos++;
                                        continue;
                                    }
                                    wait = parsedMD(lastMet, markOrd, needToFind, wait, markConv, met, t, out, mdToHTML);
                                    needToFind = (wait < markConv.size());
                                    pos++;
                                }
                                if (j != lines.size() - 1) {
                                    out.write(System.lineSeparator());
                                }
                                pos = 0;
                            }
                            if (titleLevel > 0) {
                                out.write("</h");
                                out.write(Integer.toString(titleLevel));
                                out.write(">");
                            } else {
                                out.write("</p>");
                            }
                            out.write(System.lineSeparator());
                            line = in.readLine();
                        }

                    } catch (IOException e) {
                        System.out.println("Cannot write to file" + e.getMessage());
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    System.out.println("Cannot open output file" + e.getMessage());
                }
            } catch (IOException e) {
                System.out.println("Cannot read from input file" + e.getMessage());
            } finally {
                in.close();
            }
        } catch (IOException e) {
            System.out.println("Cannot read input file" + e.getMessage());
        }
    }
}