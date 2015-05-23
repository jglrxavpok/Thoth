package org.jglr.thoth;

import java.util.HashMap;
import java.util.Map;

public interface Constants {

    String DEF_KEYWORD = "def";
    String DEF_END = "|end|";

    char FEMININE_ID = 'f';
    char MASCULINE_ID = 'm';
    char NEUTRAL_ID = 'n';
    char SINGULAR_ID = 's';
    char PLURAL_ID = 'p';

    int FLAG_FEMININE = 0b00000001;
    int FLAG_MASCULINE = 0b00000010;
    int FLAG_NEUTRAL = 0b00000100;
    int FLAG_SINGULAR = 0b00001000;
    int FLAG_PLURAL = 0b00010000;

    Map<Character, Integer> idsToFlag = new HashMap<Character, Integer>(){
        {
            put(FEMININE_ID, FLAG_FEMININE);
            put(MASCULINE_ID, FLAG_MASCULINE);
            put(NEUTRAL_ID, FLAG_NEUTRAL);
            put(SINGULAR_ID, FLAG_SINGULAR);
            put(PLURAL_ID, FLAG_PLURAL);
        }
    };

}
