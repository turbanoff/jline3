/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import java.io.File;
import java.util.List;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.reader.ParsedLine;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * A file name completer takes the buffer and issues a list of
 * potential completions.
 * <p/>
 * This completer tries to behave as similar as possible to
 * <i>bash</i>'s file name completion (using GNU readline)
 * with the following exceptions:
 * <p/>
 * <ul>
 * <li>Candidates that are directories will end with "/"</li>
 * <li>Wildcard regular expressions are not evaluated or replaced</li>
 * <li>The "~" character can be used to represent the user's home,
 * but it cannot complete to other users' homes, since java does
 * not provide any way of determining that easily</li>
 * </ul>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class FileNameCompleter
    implements Completer
{
    // TODO: Handle files with spaces in them

    private static final boolean OS_IS_WINDOWS;

    static {
        String os = System.getProperty("os.name");
        OS_IS_WINDOWS = os.contains("windows");
    }

    public int complete(ParsedLine line, final List<Candidate> candidates) {
        checkNotNull(line);
        checkNotNull(candidates);

        String buffer = line.word();
        if (buffer == null) {
            buffer = "";
        }

        if (OS_IS_WINDOWS) {
            buffer = buffer.replace('/', '\\');
        }

        String translated = buffer;

        File homeDir = getUserHome();

        // Special character: ~ maps to the user's home directory
        if (translated.startsWith("~" + separator())) {
            translated = homeDir.getPath() + translated.substring(1);
        }
        else if (translated.startsWith("~")) {
            translated = homeDir.getParentFile().getAbsolutePath();
        }
        else if (!(new File(translated).isAbsolute())) {
            String cwd = getUserDir().getAbsolutePath();
            translated = cwd + separator() + translated;
        }

        File file = new File(translated);
        final File dir;

        if (translated.endsWith(separator())) {
            dir = file;
        }
        else {
            dir = file.getParentFile();
        }

        File[] entries = dir == null ? new File[0] : dir.listFiles();

        return matchFiles(buffer, translated, entries, candidates);
    }

    protected String separator() {
        return File.separator;
    }

    protected File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    protected File getUserDir() {
        return new File(System.getProperty("user.dir"));
    }

    protected int matchFiles(final String buffer, final String translated, final File[] files, final List<Candidate> candidates) {
        if (files == null) {
            return -1;
        }

        int matches = 0;

        // first pass: just count the matches
        for (File file : files) {
            if (file.getAbsolutePath().startsWith(translated)) {
                matches++;
            }
        }
        for (File file : files) {
            if (file.getAbsolutePath().startsWith(translated)) {
                CharSequence name = file.getName() + (matches == 1 && file.isDirectory() ? separator() : " ");
                candidates.add(new Candidate(render(file, name).toString()));
            }
        }

        final int index = buffer.lastIndexOf(separator());

        return index + separator().length();
    }

    protected CharSequence render(final File file, final CharSequence name) {
        return name;
    }
}