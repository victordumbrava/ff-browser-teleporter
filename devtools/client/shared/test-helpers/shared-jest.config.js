/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

const fixturesDir = `${__dirname}/jest-fixtures`;

module.exports = {
  verbose: true,
  moduleNameMapper: {
    // Custom name mappers for modules that require m-c specific API.
    "^devtools/shared/generate-uuid": `${fixturesDir}/generate-uuid`,
    "^devtools/shared/DevToolsUtils": `${fixturesDir}/devtools-utils`,
    // This is needed for the Debugger, for some reason
    "shared/DevToolsUtils": `${fixturesDir}/devtools-utils`,

    // Mocks only used by node tests.
    "Services-mock": `${fixturesDir}/Services`,
    "ChromeUtils-mock": `${fixturesDir}/ChromeUtils`,

    "^Glean": `${fixturesDir}/Glean`,
    "^promise": `${fixturesDir}/promise`,
    "^resource://devtools/client/shared/fluent-l10n/fluent-l10n.js": `${fixturesDir}/fluent-l10n`,
    "^resource://devtools/client/shared/unicode-url.js": `${fixturesDir}/unicode-url`,
    // This is needed for the Debugger, for some reason
    "shared/unicode-url.js": `${fixturesDir}/unicode-url`,
    "shared/telemetry.js": `${fixturesDir}/telemetry`,
    "^resource://devtools/client/shared/telemetry.js": `${fixturesDir}/telemetry`,
    // This is needed for the Debugger, for some reason
    "client/shared/telemetry$": `${fixturesDir}/telemetry`,
    "devtools/shared/plural-form$": `${fixturesDir}/plural-form`,
    // Sometimes returning an empty object is enough
    "^resource://devtools/client/shared/link": `${fixturesDir}/empty-module`,
    "resource://devtools/shared/validate-breakpoint.sys.mjs": `${fixturesDir}/empty-module`,
    "resource://services-settings/remote-settings.sys.mjs": `${fixturesDir}/empty-module`,
    "^devtools/shared/flags": `${fixturesDir}/empty-module`,
    "^resource://devtools/shared/indexed-db.js": `${fixturesDir}/indexed-db`,
    "^devtools/shared/layout/utils": `${fixturesDir}/empty-module`,
    "^devtools/client/shared/components/tree/TreeView.mjs": `${fixturesDir}/empty-module`,
    "/target-command.js": `${fixturesDir}/target-command`,
    // Map all require("devtools/...") to the real devtools root.
    "^devtools/(.*)": `${__dirname}/../../../$1`,
    "^resource://devtools/(.*)": `${__dirname}/../../../$1`,
  },
  transform: {
    "\\.m?js$": "babel-jest",
  },
};
