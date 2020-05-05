/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2016 Aaron Madlon-Kay
               Home page: http://www.omegat.org/
               Support center: https://omegat.org/support

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.core.spellchecker;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.util.Collections;
import java.util.List;

import org.languagetool.rules.spelling.hunspell.Hunspell;
import org.omegat.util.Log;
import org.omegat.util.OConsts;
import org.omegat.util.Preferences;

/**
 * A thin wrapper around the LanguageTool Hunspell implementation (which itself
 * wraps native libs)
 *
 * @author Aaron Madlon-Kay
 * @author Briac Pilpre
 */
public class SpellCheckerLangToolHunspell implements ISpellCheckerProvider {
    private String dictBasename;
    private Hunspell.Dictionary dict;

    @Override
    public void init(String language) throws SpellCheckerException {
        // check that the dict exists
        String dictionaryDir = Preferences.getPreferenceDefault(Preferences.SPELLCHECKER_DICTIONARY_DIRECTORY,
                SpellChecker.DEFAULT_DICTIONARY_DIR.getPath());

        File dictBasename = new File(dictionaryDir, language);
        File affixName = new File(dictionaryDir, language + OConsts.SC_AFFIX_EXTENSION);
        File dictionaryName = new File(dictionaryDir, language + OConsts.SC_DICTIONARY_EXTENSION);

        if (!SpellChecker.isValidFile(affixName) || !SpellChecker.isValidFile(dictionaryName)) {
            // If we still don't have a dictionary then return
            throw new SpellCheckerException("No dictionary found at " + affixName + " or " + dictionaryName);
        }

        this.dictBasename = dictBasename.getPath();
        try {
            this.dict = Hunspell.getInstance().getDictionary(this.dictBasename);
        } catch (UnsatisfiedLinkError | UnsupportedOperationException | IOException e) {
            throw new SpellCheckerException("Error loading hunspell dictionary", e);
        }

        Log.log("Initialized LanguageTool Hunspell spell checker for language '" + language + "' dictionary "
                + dictBasename);
    }

    @Override
    public boolean isCorrect(String word) {
        return !dict.misspelled(word);
    }

    @Override
    public List<String> suggest(String word) {
        try {
            return dict.suggest(word);
        } catch (CharacterCodingException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void learnWord(String word) {
        try {
            dict.addWord(word);
        } catch (UnsupportedEncodingException e) {
            Log.log(e);
        }
    }

    @Override
    public void destroy() {
        dict.destroy();
        Hunspell.getInstance().destroyDictionary(dictBasename);
    }
}
