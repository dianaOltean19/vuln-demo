# vuln-demo

A deliberately vulnerable Java project for testing security scanners. **Never deploy or run it.**
It has not been compiled; it only needs to be scanned.

Contents: `pom.xml` (3 old dependencies), `src/main/java/demo/App.java` (11 planted code flaws),
`src/main/resources/application.properties` (hard-coded password).

## Code and configuration findings a scanner should report

| CWE | Name | Where |
|---|---|---|
| CWE-89 | SQL Injection | `App.java:29` |
| CWE-78 | OS Command Injection | `App.java:33` |
| CWE-22 | Path Traversal | `App.java:37` |
| CWE-328 | Use of Weak Hash (MD5) | `App.java:41` |
| CWE-338 | Cryptographically Weak PRNG (`java.util.Random` for a reset token) | `App.java:46` |
| CWE-502 | Deserialization of Untrusted Data (`ObjectInputStream`) | `App.java:50` |
| CWE-611 | XML External Entity (XXE) | `App.java:54` |
| CWE-502 | Deserialization of Untrusted Data (`new Yaml().load`) | `App.java:58` |
| CWE-94 | Code Injection (`StringSubstitutor.createInterpolator`, "Text4Shell" pattern) | `App.java:62` |
| CWE-117 | Log Output Neutralization (user input in a log line; also the Log4Shell entry point) | `App.java:66` |
| CWE-798 | Hard-coded Credentials | `App.java:24` and `application.properties:3` |

## Dependency findings a scanner should report

See `EXPECTED-DEPENDENCY-FINDINGS.csv` (17 GHSA advisories, checked against GitHub's advisory database).
The ones that matter most:

| Package | GHSA | CVE | Severity | CWEs | Fixed in |
|---|---|---|---|---|---|
| log4j-core 2.14.1 | GHSA-jfh8-c2jp-5v3q | CVE-2021-44228 (Log4Shell) | critical | 20, 400, 502, 917 | 2.15.0 |
| log4j-core 2.14.1 | GHSA-7rjr-3q55-vv33 | CVE-2021-45046 | critical | 502, 917 | 2.16.0 |
| log4j-core 2.14.1 | GHSA-p6xc-xr62-6r2g | CVE-2021-45105 | high | 20, 674 | 2.17.0 |
| commons-text 1.9 | GHSA-599f-7c49-w659 | CVE-2022-42889 (Text4Shell) | critical | 94 | 1.10.0 |
| snakeyaml 1.30 | GHSA-mjmj-j48q-9wg2 | CVE-2022-1471 | high | 20, 502 | 2.0 |
| snakeyaml 1.30 | GHSA-3mc7-4q67-w48m | CVE-2022-25857 | high | 400, 776 | 1.31 |

Totals: log4j-core 7, snakeyaml 7, commons-text 1, log4j-api 1 (indirect), commons-lang3 1 (indirect via commons-text).

## Not expected
- Findings for `org.osgi:org.osgi.core` (indirect): it has no known advisories.
- A scanner that only reads direct dependencies will miss the two indirect ones (log4j-api GHSA-qv9r-c865-cp47, commons-lang3 GHSA-j288-q9x7-2f5v).
- Advisories published after 2026-10-19 can add rows to the dependency list.
