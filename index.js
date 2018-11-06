//  Copyright © 2012-2018 3 Electric Sheep Pty Ltd. All rights reserved.
//
//  The Welcome Interruption Software Development Kit (SDK) is licensed to you subject to the terms
//  of the License Agreement. The License Agreement forms a legally binding contract between you and
//  3 Electric Sheep Pty Ltd in relation to your use of the Welcome Interruption SDK.
//  You may not use this file except in compliance with the License Agreement.
//
//  A copy of the License Agreement can be found in the LICENSE file in the root directory of this
//  source tree.
//
//  Unless required by applicable law or agreed to in writing, software distributed under the License
//  Agreement is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
//  express or implied. See the License Agreement for the specific language governing permissions
//  and limitations under the License Agreement.

import {Wiapp, WiAppListener} from "./wiapp";
import {WiConfig} from "./wiconfig";
import {WiPushMgr} from "./wipushmgr";
import {Wiapi} from "./wiapi";

let wisdkInterface = {
    // Wi high level objects
    WiApp: Wiapp,
    WiAppListener: WiAppListener,
    WiConfig: WiConfig,
    WiPushMgr:WiPushMgr,
    Wiapi: Wiapi
};

module.exports = wisdkInterface;

