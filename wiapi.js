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


export const getErrorMsg = (e) => {
    let err = e.message;

    if (e.result && e.result.code !== undefined && e.result.code !== null)
        err += ` (code ${e.result.code})`;

    if (e.result && e.result.errors) {
        let _flatten = (e) => {
            return (e.path && e.msg) ? e.path.join('.') + ':' + e.msg: e+'';
        };

        err += '\n' + e.result.errors.map(_flatten).join('\n');
    }

    return err;
};

export const utcnow = () => {
    let now = new Date();
    return now.toISOString();
};

export class Wiapi {
    constructor(endpoint){
        this.endpoint = endpoint;
        this.headers = null;
        this.accessToken = null;
        this.authType = null;
        this.accessSystemDefaults = null;

    }

    getEndpoint = () => {
        return this.endpoint;
    };

    setEndpoint = (endpoint) => {
        this.endpoint = endpoint;
    };

    /**
     returns whether the user has an authorziation token or not
     */
    isAuthorized = () => {
        return this.accessToken !== null && this.accessToken !== undefined && this.accessToken;
    };

    clearAuth = () => {
        this.accessToken = null;
    };

    call = (method, url, body, auth, wrap) => {
        let params = {
            method: method
        };

        if (auth === undefined)
            auth = (self.accessToken != null);

        if (wrap === undefined)
            wrap = true;

        if (body != null) {
            params.body = JSON.stringify(body);
            params.headers  =  {
                "Content-type": "application/json"
            }
        }
        if (this.headers != null){
            params.headers = {...params.headers, ...this.headers}
        }

        let token = '';
        if (auth && this.accessToken){
            if (url.indexOf('?')>=0){
                token = "&token=" + this.accessToken;
            }
            else {
                token = "?token=" + this.accessToken;
            }
        }
        let fullurl = `${this.endpoint}${(url.indexOf('/')!==0)?'/':''}${url}${token}`;

        return fetch(fullurl, params).then((response) => {
            if (response.ok) {
                const contentType = response.headers.get("content-type");
                if(contentType && contentType.indexOf("application/json") !== -1) {
                    return response.json();
                }
                else if(contentType && contentType.indexOf("text/") !== -1) {
                    return response.text();
                }
                else {
                    return response.blob();
                }
            }
            else {
                throw this.wrapApiException(`${(response.statusText)?response.statusText:"HTTP Error"} (${response.status})`, null,  response);
            }
        });
    };

    /**
     * Like normal api call but returns a promise and deals with soft error
     * @param method
     * @param url
     * @param body
     * @param auth
     */
    callapi = (method, url, body, auth) => {
        return this.call(method, url, body, auth, true).then((result)=>{
            return new Promise((resolve, reject)=> {
                if (result.success)
                    resolve(result);
                else
                    reject(this.wrapApiException(result.msg, result, null));
            });
        });
    };

    /**
     * Used for RSAA actions - returns a dict with a method, endpoint, body, headers etc..
     * @param method
     * @param url
     * @param body
     * @param auth
     */
    callaction = (method, url, body, auth) => {
        let params = {
            method: method
        };

        if (auth === undefined)
            auth = (self.accessToken != null);

        if (body != null) {
            params.body = JSON.stringify(body);
            params.headers  =  {
                "Content-type": "application/json"
            }
        }
        if (this.headers != null){
            params.headers = {...params.headers, ...this.headers}
        }

        let token = '';
        if (auth && this.accessToken){
            if (url.indexOf('?')>=0){
                token = "&token=" + this.accessToken;
            }
            else {
                token = "?token=" + this.accessToken;
            }
        }

        params.endpoint = `${this.endpoint}${(url.indexOf('/')!==0)?'/':''}${url}${token}`;

        return params;
    };

    callurl = (url, auth) => {
        let token = '';
        if (auth && this.accessToken){
            if (url.indexOf('?')>=0){
                token = "&token=" + this.accessToken;
            }
            else {
                token = "?token=" + this.accessToken;
            }
        }
        return  `${this.endpoint}${(url.indexOf('/')!==0)?'/':''}${url}${token}`;
    };

    wrapApiException = (msg, result, response) => {
        let err = new Error(msg);
        if (result) {
            const {success, ...extra} = result;
            err.extra = extra;
        }


        if (response !== undefined)
            err.response = response;

        return err;
    };


}
