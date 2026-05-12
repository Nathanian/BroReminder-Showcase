package com.bro.broreminder.voice;

import java.util.*;

/**
 * Normalizes and parses German date and time phrases for the voice wizard.
 * All methods are pure and locale aware.
 */
public final class GermanDateTimeNormalizer {
    private GermanDateTimeNormalizer() {}

    private static final Locale DEFAULT_LOCALE = Locale.GERMANY;
    private static final TimeZone DEFAULT_TZ = TimeZone.getTimeZone("Europe/Berlin");

    private static final Set<String> FILLERS = new HashSet<>(Arrays.asList(
            "am", "den", "der", "im", "um"
    ));

    /** Normalizes raw ASR text to a simplified ascii form. */
    public static String normalizeText(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(DEFAULT_LOCALE);
        s = s.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        s = s.replaceAll("\\s+", " ");
        // split glued "17uhr" or "siebzehnuhr"
        s = s.replaceAll("(\\d+)uhr", "$1 uhr");
        s = s.replaceAll("([a-z]+)uhr", "$1 uhr");
        for (String f : FILLERS) s = s.replaceAll("\\b" + java.util.regex.Pattern.quote(f) + "\\b", " ");
        s = s.replaceAll(" +", " ").trim();
        return s;
    }

    /* ---------- Number parsing ---------- */

    private static final Map<String, Integer> CARDINALS = new HashMap<>();
    private static final Map<String, Integer> TENS = new HashMap<>();
    static {
        String[][] base = {
                {"null", "0"}, {"eins", "1"}, {"ein", "1"}, {"zwei", "2"},
                {"zwo", "2"}, {"drei", "3"}, {"vier", "4"}, {"fuenf", "5"}, {"sechs", "6"},
                {"sieben", "7"}, {"acht", "8"}, {"neun", "9"}, {"zehn", "10"},
                {"elf", "11"}, {"zwoelf", "12"}, {"dreizehn", "13"},
                {"vierzehn", "14"}, {"fuenfzehn", "15"}, {"sechzehn", "16"},
                {"siebzehn", "17"}, {"achtzehn", "18"}, {"neunzehn", "19"}
        };
        for (String[] p : base) CARDINALS.put(p[0], Integer.parseInt(p[1]));
        String[][] tens = {
                {"zwanzig", "20"}, {"dreissig", "30"}, {"vierzig", "40"},
                {"fuenfzig", "50"}, {"sechzig", "60"}, {"siebzig", "70"},
                {"achtzig", "80"}, {"neunzig", "90"}
        };
        for (String[] p : tens) TENS.put(p[0], Integer.parseInt(p[1]));
    }

    private static final String[] ORDINAL_SUFFIXES = {
            "te", "ten", "ter", "tem", "tes", "ste", "sten", "ster", "stem", "stes"
    };
    private static final Map<String,Integer> ORDINAL_STEMS = new HashMap<>();
    static {
        ORDINAL_STEMS.put("erst", 1);     // erster/erste/ersten …
        ORDINAL_STEMS.put("zweit", 2);
        ORDINAL_STEMS.put("dritt", 3);
        ORDINAL_STEMS.put("viert", 4);
        ORDINAL_STEMS.put("fuenft", 5);
        ORDINAL_STEMS.put("sechst", 6);
        ORDINAL_STEMS.put("siebt", 7);
        ORDINAL_STEMS.put("acht", 8);     // achten/achter/achte
        ORDINAL_STEMS.put("neunt", 9);
        ORDINAL_STEMS.put("zehnt", 10);
        ORDINAL_STEMS.put("elft", 11);
        ORDINAL_STEMS.put("zwoelft", 12);
    }
    /** Parses German number words or ordinals to an int, -1 on failure. */
    public static int parseGermanNumber(String tokenOrPhrase, int max) {
        if (tokenOrPhrase == null) return -1;

        // normalize (ae/oe/ue/ss, lowercase, trim, collapse spaces, split glued forms)
        String s = normalizeText(tokenOrPhrase);
        if (s.isEmpty()) return -1;

        // remove non-alphanumerics (e.g., "17." -> "17", "dreißig-" -> "dreissig")
        s = s.replaceAll("[^a-z0-9]", "");

        // pure digits
        if (s.matches("\\d+")) {
            try {
                int dv = Integer.parseInt(s);
                return (dv <= max) ? dv : -1;
            } catch (NumberFormatException ignored) { /* fall through */ }
        }

        // strip common ordinal suffixes (ersten/erster/erstem/erstes, achten/achter/achte, …)
        for (String suf : ORDINAL_SUFFIXES) {
            if (s.endsWith(suf)) {
                s = s.substring(0, s.length() - suf.length());
                break;
            }
        }

        // ordinal stems first (covers "erst", "dritt", "acht", "siebt", …)
        Integer ov = ORDINAL_STEMS.get(s);
        if (ov != null && ov <= max) return ov;

        // exact cardinal words (eins, zwei, drei, … neunzehn)
        Integer cv = CARDINALS.get(s);
        if (cv != null && cv <= max) return cv;

        // exact tens (zwanzig, dreissig, vierzig, … neunzig)
        Integer tv = TENS.get(s);
        if (tv != null && tv <= max) return tv;

        // "einundzwanzig" / "fuenfundvierzig" (ones + tens with "und")
        int idx = s.indexOf("und");
        if (idx > 0) {
            int ones = parseGermanNumber(s.substring(0, idx), 9);
            int tens = parseGermanNumber(s.substring(idx + 3), 99);
            if (ones >= 0 && tens >= 0) {
                int sum = ones + tens;
                return (sum <= max) ? sum : -1;
            }
        }

        return -1;
    }

    /** Parses a year between 1900 and 2099. */
    public static int parseYear(String token) {
        String s = normalizeText(token);
        if (s.matches("\\d{4}")) {
            int y = Integer.parseInt(s);
            if (y >= 1900 && y <= 2099) return y;
        }
        if (s.startsWith("neunzehnhundert")) {
            String r = s.substring("neunzehnhundert".length());
            if (r.isEmpty()) return 1900;
            int n = parseGermanNumber(r, 99);
            if (n >= 0) return 1900 + n;
        }
        if (s.startsWith("zweitausend")) {
            String r = s.substring("zweitausend".length());
            if (r.isEmpty()) return 2000;
            int n = parseGermanNumber(r, 99);
            if (n >= 0) return 2000 + n;
        }
        return -1;
    }

    /* ---------- Date parsing ---------- */

    public static Long parseDate(String text, TimeZone tz, Locale locale) {
        if (tz == null) tz = DEFAULT_TZ;
        if (locale == null) locale = DEFAULT_LOCALE;
        String norm = normalizeText(text);
        Calendar now = Calendar.getInstance(tz, locale);
        Long r = parseRelativeDate(norm, now); if (r != null) return r;
        Long e = parseExplicitDate(norm, now); if (e != null) return e;
        Long w = parseWeekday(norm, now); if (w != null) return w;
        return null;
    }

    private static Long parseRelativeDate(String norm, Calendar now) {
        if (norm.equals("heute")) {
            Calendar c = (Calendar) now.clone(); clearTime(c); return c.getTimeInMillis();
        }
        if (norm.equals("morgen")) {
            Calendar c = (Calendar) now.clone(); c.add(Calendar.DAY_OF_MONTH, 1); clearTime(c); return c.getTimeInMillis();
        }
        if (norm.equals("uebermorgen")) {
            Calendar c = (Calendar) now.clone(); c.add(Calendar.DAY_OF_MONTH, 2); clearTime(c); return c.getTimeInMillis();
        }
        String[] t = norm.split(" ");
        for (int i = 0; i < t.length; i++) {
            String cur = t[i];
            if (cur.startsWith("uebernaechst") && i + 1 < t.length) {
                Integer wd = WEEKDAY_MAP.get(t[i + 1]);
                if (wd != null) {
                    Calendar c = nextWeekday(now, wd, true);
                    c.add(Calendar.DAY_OF_MONTH, 7);
                    return c.getTimeInMillis();
                }
            }
            if (cur.startsWith("naechst") && i + 1 < t.length) {
                Integer wd = WEEKDAY_MAP.get(t[i + 1]);
                if (wd != null) {
                    Calendar c = nextWeekday(now, wd, true);
                    return c.getTimeInMillis();
                }
            }
            if (cur.equals("in") && i + 2 < t.length) {
                int val = parseGermanNumber(t[i + 1], 99);
                String unit = t[i + 2];
                if (val >= 0) {
                    Calendar c = (Calendar) now.clone();
                    if (unit.startsWith("tag")) {
                        c.add(Calendar.DAY_OF_MONTH, val);
                        clearTime(c); return c.getTimeInMillis();
                    } else if (unit.startsWith("woche")) {
                        c.add(Calendar.DAY_OF_MONTH, val * 7);
                        clearTime(c); return c.getTimeInMillis();
                    }
                }
            }
        }
        return null;
    }

    private static Long parseExplicitDate(String norm, Calendar now) {
        String[] parts = norm.replace('.', ' ').split(" ");
        int day = -1, month = -1, year = -1;
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (day < 0) {
                int d = parseGermanNumber(p, 31);
                if (d > 0) { day = d; continue; }
            }
            if (month < 0) {
                Integer m = MONTH_MAP.get(p);
                if (m != null) { month = m; continue; }
                int mm = parseGermanNumber(p, 12);
                if (mm > 0) { month = mm; continue; }
            }
            if (year < 0) {
                int y = parseYear(p);
                if (y >= 0) year = y;
            }
        }
        if (day > 0 && month > 0) {
            Calendar c = Calendar.getInstance(now.getTimeZone(), DEFAULT_LOCALE);
            c.set(Calendar.YEAR, year > 0 ? year : now.get(Calendar.YEAR));
            c.set(Calendar.MONTH, month - 1);
            c.set(Calendar.DAY_OF_MONTH, day);
            clearTime(c);
            if (year <= 0 && !c.after(now)) c.add(Calendar.YEAR, 1);
            try { c.getTime(); } catch (Exception e) { return null; }
            return c.getTimeInMillis();
        }
        return null;
    }

    private static Long parseWeekday(String norm, Calendar now) {
        Integer wd = WEEKDAY_MAP.get(norm);
        if (wd == null) return null;
        Calendar c = nextWeekday(now, wd, false);
        return c.getTimeInMillis();
    }

    private static Calendar nextWeekday(Calendar now, int target, boolean strictAfter) {
        Calendar c = Calendar.getInstance(now.getTimeZone(), DEFAULT_LOCALE);
        c.setTime(now.getTime());
        int today = now.get(Calendar.DAY_OF_WEEK);
        int diff = target - today;
        if (diff < 0) diff += 7;       // allow 0 when not strict
        if (strictAfter && diff == 0) diff = 7;
        c.add(Calendar.DAY_OF_MONTH, diff);
        clearTime(c);
        return c;
    }

    /* ---------- Time parsing ---------- */

    public static boolean applyTime(String text, Calendar when) {
        String norm = normalizeText(text);
        return parseTimeInternal(norm, when);
    }

    private static boolean parseTimeInternal(String norm, Calendar when) {
        if (norm.equals("mitternacht") || norm.equals("nulluhr") || norm.equals("null uhr")) {
            setTime(when, 0, 0); return true;
        }
        if (norm.equals("mittag") || norm.equals("mittags")) {
            setTime(when, 12, 0); return true;
        }
        String[] tokens = norm.split(" ");
        if (tokens.length >= 2 && tokens[0].equals("halb")) {
            int h = parseGermanNumber(tokens[1], 23);
            if (h >= 0) { h = (h + 23) % 24; setTime(when, h, 30); return true; }
        }
        if (tokens.length >= 3 && tokens[0].equals("viertel") && tokens[1].equals("nach")) {
            int h = parseGermanNumber(tokens[2], 23);
            if (h >= 0) { setTime(when, h, 15); return true; }
        }
        if (tokens.length >= 3 && tokens[0].equals("viertel") && tokens[1].equals("vor")) {
            int h = parseGermanNumber(tokens[2], 23);
            if (h >= 0) { h = (h + 23) % 24; setTime(when, h, 45); return true; }
        }
        List<String> list = new ArrayList<>(Arrays.asList(tokens));
        // remove filler tokens that might precede or follow numbers
        list.remove("uhr");
        list.remove("und");
        list.remove("minute");
        list.remove("minuten");
        if (list.isEmpty()) return false;
        int h = parseGermanNumber(list.get(0), 23);
        if (h < 0) return false;
        int m = 0;
        if (list.size() > 1) {
            // search for the first parsable minute token after the hour
            for (int i = 1; i < list.size(); i++) {
                int mm = parseGermanNumber(list.get(i), 59);
                if (mm >= 0) { m = mm; break; }
            }
        }
        setTime(when, h, m);
        return true;
    }

    private static void setTime(Calendar c, int h, int m) {
        c.set(Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, h)));
        c.set(Calendar.MINUTE, Math.max(0, Math.min(59, m)));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static void clearTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    /* ---------- Lookup tables ---------- */

    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("januar", 1); MONTH_MAP.put("jan", 1);
        MONTH_MAP.put("februar", 2); MONTH_MAP.put("feb", 2);
        MONTH_MAP.put("maerz", 3); MONTH_MAP.put("maer", 3); MONTH_MAP.put("mar", 3);
        MONTH_MAP.put("april", 4); MONTH_MAP.put("apr", 4);
        MONTH_MAP.put("mai", 5);
        MONTH_MAP.put("juni", 6); MONTH_MAP.put("jun", 6);
        MONTH_MAP.put("juli", 7); MONTH_MAP.put("jul", 7);
        MONTH_MAP.put("august", 8); MONTH_MAP.put("aug", 8);
        MONTH_MAP.put("september", 9); MONTH_MAP.put("sep", 9);
        MONTH_MAP.put("oktober", 10); MONTH_MAP.put("okt", 10);
        MONTH_MAP.put("november", 11); MONTH_MAP.put("nov", 11);
        MONTH_MAP.put("dezember", 12); MONTH_MAP.put("dez", 12);
    }

    private static final Map<String, Integer> WEEKDAY_MAP = new HashMap<>();
    static {
        WEEKDAY_MAP.put("montag", Calendar.MONDAY);
        WEEKDAY_MAP.put("dienstag", Calendar.TUESDAY);
        WEEKDAY_MAP.put("mittwoch", Calendar.WEDNESDAY);
        WEEKDAY_MAP.put("donnerstag", Calendar.THURSDAY);
        WEEKDAY_MAP.put("freitag", Calendar.FRIDAY);
        WEEKDAY_MAP.put("samstag", Calendar.SATURDAY);
        WEEKDAY_MAP.put("sonntag", Calendar.SUNDAY);
    }
}
