package com.springboot.knockbe;

public final class MoneyParser{
    private MoneyParser(){}

    public static long toWon(String raw){
        if (raw == null) return 0L;
        String s = raw.replace(",", "").replace(" ", "").replace("원","").trim();

        long won = 0;
        int eok = s.indexOf("억");
        if (eok >= 0){
            String left = s.substring(0, eok);
            if (!left.isBlank()){
                won += Math.round(Double.parseDouble(left) * 100_000_000L);
            }
            String right = s.substring(eok+1);
            if (!right.isBlank()){
                int man = right.indexOf("만");
                if (man >= 0){
                    won += Long.parseLong(right.substring(0, man)) * 10_000L;
                } else if (right.matches("\\d+")){
                    won += Long.parseLong(right) * 10_000L; // "3억8000"
                }
            }
            return won;
        }
        int man = s.indexOf("만");
        if (man >= 0){
            return Long.parseLong(s.substring(0, man)) * 10_000L;
        }
        return Long.parseLong(s); // 이미 원 단위
    }
}