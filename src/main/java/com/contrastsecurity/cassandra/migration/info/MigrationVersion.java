/**
 * Copyright 2010-2015 Axel Fontaine
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrastsecurity.cassandra.migration.info;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MigrationVersion implements Comparable<MigrationVersion> {

    public static final MigrationVersion EMPTY = new MigrationVersion(null, "<< Empty Schema >>");
    public static final MigrationVersion LATEST = new MigrationVersion(BigInteger.valueOf(-1), "<< Latest Version >>");
    public static final MigrationVersion CURRENT = new MigrationVersion(BigInteger.valueOf(-2), "<< Current Version >>");

    public static final String TABLE = "migration_version";
    private List<BigInteger> versionParts;
    private String displayText;

    private static Pattern splitPattern = Pattern.compile("\\.(?=\\d)");

    public MigrationVersion(BigInteger version, String displayText) {
        List<BigInteger> tmp = new ArrayList<>();
        tmp.add(version);
        this.displayText = displayText;
        init(tmp, displayText);
    }

    private MigrationVersion(String version) {
        String normalizedVersion = version.replace('_', '.');
        init(tokenize(normalizedVersion), normalizedVersion);
    }

    private void init(List<BigInteger> versionParts, String displayText) {
        this.versionParts = versionParts;
        this.displayText = displayText;
    }

    public static MigrationVersion fromVersion(String version) {
        if ("current".equalsIgnoreCase(version)) return CURRENT;
        if (LATEST.getVersion().equals(version)) return LATEST;
        if (version == null) return EMPTY;
        return new MigrationVersion(version);
    }

    public String getVersion() {
        if (this.equals(EMPTY)) return null;
        if (this.equals(LATEST)) return Long.toString(Long.MAX_VALUE);
        return displayText;
    }

    /**
     * @return The textual representation of the version.
     */
    @Override
    public String toString() {
        return displayText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MigrationVersion version1 = (MigrationVersion) o;

        return compareTo(version1) == 0;
    }

    @Override
    public int hashCode() {
        return versionParts == null ? 0 : versionParts.hashCode();
    }

    @Override
    public int compareTo(MigrationVersion o) {
        if (o == null) {
            return 1;
        }

        if (this == EMPTY) {
            return o == EMPTY ? 0 : Integer.MIN_VALUE;
        }

        if (this == CURRENT) {
            return o == CURRENT ? 0 : Integer.MIN_VALUE;
        }

        if (this == LATEST) {
            return o == LATEST ? 0 : Integer.MAX_VALUE;
        }

        if (o == EMPTY) {
            return Integer.MAX_VALUE;
        }

        if (o == CURRENT) {
            return Integer.MAX_VALUE;
        }

        if (o == LATEST) {
            return Integer.MIN_VALUE;
        }
        final List<BigInteger> elements1 = versionParts;
        final List<BigInteger> elements2 = o.versionParts;
        int largestNumberOfElements = Math.max(elements1.size(), elements2.size());
        for (int i = 0; i < largestNumberOfElements; i++) {
            final int compared = getOrZero(elements1, i).compareTo(getOrZero(elements2, i));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private BigInteger getOrZero(List<BigInteger> elements, int i) {
        return i < elements.size() ? elements.get(i) : BigInteger.ZERO;
    }

    public String getTable() {
        return TABLE;
    }

    private void setVersion(String version) {
        String normalizedVersion = version.replace('_', '.');
        this.versionParts = tokenize(normalizedVersion);
        this.displayText = normalizedVersion;
    }

    private List<BigInteger> tokenize(String str) {
        List<BigInteger> numbers = new ArrayList<>();
        for (String number : splitPattern.split(str)) {
            try {
                numbers.add(new BigInteger(number));
            } catch (NumberFormatException e) {
                throw new CassandraMigrationException(
                        "Invalid version containing non-numeric characters. Only 0..9 and . are allowed. Invalid version: "
                                + str);
            }
        }
        for (int i = numbers.size() - 1; i > 0; i--) {
            if (!numbers.get(i).equals(BigInteger.ZERO)) break;
            numbers.remove(i);
        }
        return numbers;
    }
}
