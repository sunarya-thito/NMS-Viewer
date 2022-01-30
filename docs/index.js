const BASE = 'https://api.github.com';
const MAX_SEARCH = 100;
let allClasses = {};
let packageList;
let searchInput;
let maximumElement = 'Reached Maximum Result Search (' + MAX_SEARCH + ' Items)';
let viewport;
let tabPanel;
let emptySearch;
let maxSearch;
let lastVersion;
let statusLoader;
let welcomeScreen;

let tabs = [];

// do it now
window.onload = () => {
    packageList = document.getElementById('PackageViewer');
    searchInput = document.getElementById('SearchInput');
    tabPanel = document.getElementById('TabButtons');
    viewport = document.getElementById('TabContent');
    statusLoader = document.getElementById('StatusLoader');
    welcomeScreen = document.getElementById('WelcomeScreen');
    document.getElementById('BarLoader').style.width = '100%';
    emptySearch = document.createElement('div');
    emptySearch.className = 'EmptySearch NonSelectable';
    emptySearch.innerHTML = 'There is nothing in here...';
    maxSearch = document.createElement('div');
    maxSearch.innerHTML = 'Max Result Search Reached';
    maxSearch.className = 'MaxSearch NonSelectable';
    searchInput.addEventListener('input', () => {
        search(searchInput.value.toLowerCase());
    });
    Rainbow.onHighlight(block => console.log(block));
    fetchAll();
};

function distinguishSameTabName() {
    for (let i in tabs) {
        let tab = tabs[i];
        let duplicates = findTabByName(tab.pack.simpleName);
        if (duplicates.length > 1) {
            tab.tabTitle.innerHTML = tab.pack.packageName + '.' + tab.pack.simpleName;
        } else {
            tab.tabTitle.innerHTML = tab.pack.simpleName;
        }
    }
}

function findTabByName(name) {
    let found = [];
    for (let i in tabs) {
        let tab = tabs[i];
        if (tab.pack.simpleName === name) {
            found.push(tab);
        }
    }
    return found;
}

function hasVersion(versions, versionString) {
    for (let i in versions) {
        if (versions[i].toString() === versionString.toString()) {
            return true;
        }
    }
    return false;
}

function enableHighlightHelper(pack, doc, version) {
    let imports = {};
    let packageName = '';
    doc.querySelectorAll('.support.namespace').forEach(element => {
        if (!element.previousElementSibling) return;
        if (element.previousElementSibling.innerHTML === 'package') {
            packageName = element.innerHTML;
            if (packageName.endsWith(';')) {
                packageName = packageName.substring(0, packageName.length - 1);
            }
            return;
        }
        if (element.previousElementSibling.innerHTML !== 'import') return;
        let html = element.innerHTML;
        if (html && html.endsWith(';')) {
            html = html.substring(0, html.length - 1);
            let packageName;
            let simpleName;
            let index = html.lastIndexOf('.');
            if (index >= 0) {
                packageName = html.substring(0, index);
                simpleName = html.substring(index + 1);
            } else {
                simpleName = html;
                packageName = '';
            }
            imports[simpleName] = {
                simpleName: simpleName,
                packageName: packageName,
                name: html
            };
        }
    });
    doc.querySelectorAll('.entity.class').forEach(element => {
        let previousSibling = element.previousSibling;
        let fullClassName;
        if (previousSibling.nodeName === '#text' && previousSibling.nodeValue && previousSibling.nodeValue.endsWith('.')) {
            fullClassName = previousSibling.nodeValue + '.' + element.innerHTML;
        } else {
            let imported = imports[element.innerHTML];
            if (imported) {
                fullClassName = imported.name;
            } else {
                fullClassName = packageName + '.' + element.innerHTML;
                let found = allClasses[fullClassName];
                if (!found || !hasVersion(found.versions, version)) {
                    fullClassName = pack.packageName + '.' +pack.simpleName;
                }
            }
        }
        element.onmouseover = () => {
            element.style.backgroundColor = 'yellow';
        };
        element.onclick = () => {
            let theClass = allClasses[fullClassName];
            console.log(fullClassName);
            if (theClass) {
                openTab(fullClassName).openVersion(version);
            }
        };
        element.onmouseleave = () => {
            element.style.backgroundColor = null;
        };
    });
}

function openTab(className) {
    let pack = allClasses[className];
    for (let i in tabs) {
        let tab = tabs[i];
        if (tab.pack === pack) {
            switchTab(tab);
            return tab;
        }
    }
    let content = document.createElement('div');
    let versionSelector = document.createElement('div');
    let codeCache = {};
    content.className = 'Viewport NonSelectable';
    let dropDownButton = document.createElement('button');
    dropDownButton.id = 'VersionButton';
    dropDownButton.className = 'DropDownButton';
    let dropDownContent = document.createElement('div');
    let closeListener = event => {
        if (event.target !== dropDownButton) {
            dropDownContent.style.display = 'none';
            document.removeEventListener('click', closeListener);
            event.stopPropagation();
        }
    };
    dropDownButton.onclick = () => {
        if (dropDownContent.style.display !== 'block') {
            dropDownContent.style.display = 'block';
            document.addEventListener('click', closeListener);
        } else {
            dropDownContent.style.display = 'none';
            document.removeEventListener('click', closeListener);
        }
    };
    // versionSelector.onfocusout = closeListener;
    // versionSelector.onmouseleave = closeListener;
    dropDownContent.className = 'DropDownContent';
    versionSelector.appendChild(dropDownButton);
    versionSelector.appendChild(dropDownContent);
    versionSelector.className = 'VersionSelector DropDown NonSelectable';
    let openVersion = function(version) {
        let elements = content.getElementsByClassName('CodeViewport');
        for (let i = 0; i < elements.length; i++) {
            elements[i].style.display = 'none';
        }
        let codeContent = codeCache[version];
        if (!codeContent) {
            let hint = content.getElementsByClassName('HintScreen');
            for (let i = 0; i < hint.length; i++) {
                content.removeChild(hint[i]);
            }
            codeCache[version] = codeContent = document.createElement('pre');
            codeContent.className = 'CodeViewport line-numbers match-braces';
            codeContent.id = 'CodePanel';
            content.appendChild(codeContent);
            openAtCurrentViewport(codeContent, pack, lastVersion);
        } else {
            codeContent.style.display = 'block';
        }
        dropDownButton.innerHTML = version;
        return codeContent
    }
    for (let i in pack.versions) {
        let version = pack.versions[i];
        let versionDiv = document.createElement('div');
        versionDiv.className = 'Version';
        versionDiv.innerHTML = version;
        versionDiv.onclick = () => {
            lastVersion = version;
            openVersion(version);
        };
        dropDownContent.appendChild(versionDiv);
    }
    content.appendChild(versionSelector);
    viewport.appendChild(content);
    if (lastVersion && hasVersion(pack.versions, lastVersion)) {
        console.log('Loading last version: ' + lastVersion);
        openVersion(lastVersion);
    } else {
        dropDownButton.innerHTML = 'Select Version';
        let hint = document.createElement('div');
        hint.className = 'HintScreen';
        hint.innerHTML = 'Select the version for '+pack.packageName + '.' + pack.simpleName;
        content.appendChild(hint);
    }
    let tabButton = document.createElement('div');
    let closeButton = document.createElement('div');
    let tabTitle = document.createElement('div');
    tabTitle.className = 'TabTitle';
    closeButton.innerHTML = '✖';
    closeButton.className = 'CloseButton NonSelectable';
    tabTitle.innerHTML = pack.simpleName;
    tabButton.className = 'TabButton NonSelectable';
    tabButton.appendChild(tabTitle);
    tabButton.appendChild(closeButton);
    tabPanel.appendChild(tabButton);
    let tab = {
        tabTitle: tabTitle,
        pack: pack,
        button: tabButton,
        html: content,
        openVersion: openVersion
    };
    if (welcomeScreen.parentNode) {
        welcomeScreen.parentNode.removeChild(welcomeScreen);
    }
    tabs.push(tab);
    switchTab(tab);
    tabButton.onclick = event => {
        switchTab(tab);
    };
    tabButton.onmousedown = event => {
        if (event && (event.which === 2 || event.button === 4)) {
            closeTab(tab);
        }
    }
    closeButton.onclick = () => {
        closeTab(tab);
    };
    distinguishSameTabName();
    return tab;
}

function setStatus(status) {
    statusLoader.innerHTML = status;
}

function openAtCurrentViewport(codeView, pack, version) {
    codeView.innerHTML = '<div class="HintScreen">Loading content...</div>';
    console.log('Opening at viewport: ' + pack.path + ' (' + version + ')');
    let response = fetch('https://raw.githubusercontent.com/sunarya-thito/NMS-Viewer/master/sources/' + version + '/' + pack.path);
    response.then(data => {
        if (response.status === 404) throw new Error();
        data.text().then(dataText => {
            codeView.innerHTML = '<div class="HintScreen">Painting...</div>';
            // codeView.innerHTML = Prism.highlight(dataText, Prism.languages.java, 'java');
            // codeView.innerHTML = '<code data-language="java">' + dataText + '</code>';
            Rainbow.color(dataText, 'java', newDataText => {
                codeView.innerHTML = newDataText;
                enableHighlightHelper(pack, codeView, version);
            });
        });
    }).catch(error => {
        codeView.innerHTML = '<div class="HintScreen">Failed to load the content<br>' + error + '</div>';
    });
}

function closeTab(tab) {
    let index = tabs.indexOf(tab);
    if (index < 0) return;
    tab.html.parentNode.removeChild(tab.html);
    tab.button.parentNode.removeChild(tab.button);
    tabs.splice(index, 1);
    if (tabs.length === 0) {
        viewport.appendChild(welcomeScreen);
        return;
    }
    if (tab.html.style.display === 'block') {
        switchTab(tabs[index > 0 ? index - 1 : 0]);
    }

    distinguishSameTabName();
}

function switchTab(tab) {
    if (tabs.indexOf(tab) < 0) return;
    for (let i in tabs) {
        let other = tabs[i];
        if (other === tab) {
            tab.button.style.backgroundColor = '#4CAF50';
            tab.html.style.display = 'block';
        } else {
            other.button.style.backgroundColor = '#358037';
            other.html.style.display = 'none';
        }
    }
}

function addPackage(version, path) {
    let className = path.substring(0, path.length - 5).replace(/\//g, '.');
    // let data = {
    // version: version,
    // url: 'https://raw.githubusercontent.com/nms-code/' + version + '/master/' + path,
    // };
    let simpleName;
    if (allClasses[className]) {
        let versions = allClasses[className].versions;
        versions.push(version);
        versions.sort((a, b) => a.index - b.index);
    } else {
        let packageName;
        let last = className.lastIndexOf('.');
        if (last >= 0) {
            simpleName = className.substring(last + 1);
            packageName = className.substring(0, last);
        } else {
            packageName = '';
            simpleName = className;
        }
        let divContent = document.createElement('div');
        let divPackage = document.createElement('div');
        let divClass = document.createElement('div');
        divContent.id = className;
        divContent.className = 'Package NonSelectable';
        divPackage.innerHTML = packageName;
        divPackage.className = 'PackageName';
        divClass.innerHTML = simpleName;
        divClass.className = 'ClassName';
        divContent.appendChild(divPackage);
        divContent.appendChild(divClass);
        divContent.onclick = event => {
            openTab(className);
        };
        allClasses[className] = {
            versions: [version],
            simpleName: simpleName,
            packageName: packageName,
            path: path,
            html: divContent
        };
    }
}

function search(keyword) {
    let packs = [];
    if (keyword.length <= 0) {
        let count = 0;
        for (let i in allClasses) {
            let cl = allClasses[i];
            packs.push(cl);
            count++;
            if (count >= MAX_SEARCH) break;
        }
        updateContent(packs);
        if (count >= MAX_SEARCH) {
            packageList.appendChild(maxSearch);
        }
        return;
    }
    for (let i in allClasses) {
        let cl = allClasses[i];
        if (i.toLowerCase().includes(keyword)) {
            packs.push(cl);
            // count++;
            // if (count >= MAX_SEARCH) {
            //     break;
            // }
        }
    }
    packs.sort((a, b) => {
        return similarity(keyword, b.simpleName) - similarity(keyword, a.simpleName);
    });
    packs = packs.slice(0, MAX_SEARCH);
    if (packs.length === 0 && keyword.length > 0) {
        packageList.innerHTML = '';
        packageList.appendChild(emptySearch);
        return;
    }
    updateContent(packs);
    if (packs.length >= MAX_SEARCH) {
        packageList.appendChild(maxSearch);
    }
}

function versionToInteger(version) {
    let split = version.split("_");
    let num = 0;
    let digitIndex = split.length;
    for (let i in split) {
        split[i] = split[i].replace(/\D/g,'');
        num += parseInt(split[i]) * Math.pow(10000, digitIndex);
        digitIndex--;
    }
    return num;
}

async function fetchAll() {
    //getData(BASE + '/repos/nms-code/1_10_R1/git/trees/master?recursive=10').then(data=>console.log(data));
    setStatus('Loading...');
    console.log('Loading NMS-Viewer repository...');
    getData(BASE + '/repos/sunarya-thito/NMS-Viewer/git/trees/master?recursive=40').then(repository => {
        let tree = repository.tree;
        for (let t in tree) {
            let path = tree[t].path;
            if (path && path.startsWith("sources/") && path.endsWith('.java')) {
                path = path.substring(8);
                let versionName = path.substring(0, path.indexOf('/'));
                let version = {
                    index: versionToInteger(versionName),
                    toString: () => versionName
                }
                addPackage(version, path.substring(versionName.length + 1));
            }
        }
        console.log('Loaded ' + tree.length + ' classes!');
        setStatus('Done');
        console.log('Total ' + Object.keys(allClasses).length + ' classpaths loaded into your browser!');
        document.getElementById('Container').style.display = 'flex';
        search(searchInput.value.toLowerCase());
        setTimeout(() => {
            document.getElementById('LoaderContainer').style.opacity = '0';
            document.getElementById('Container').style.opacity = '100';
            setTimeout(() => {
                document.body.removeChild(document.getElementById('LoaderContainer'));
            }, 1000);
        }, 1500);
    });
}

function updateContent(packs) {
    packageList.innerHTML = '';
    for (let j in packs) {
        let pack = packs[j];
        let div = pack.html;
        packageList.appendChild(div);
    }
}

async function getData(url = '') {
    // Default options are marked with *
    try {
        const response = await fetch(url, {
            method: 'GET', // *GET, POST, PUT, DELETE, etc.
            headers: {
                Accept: 'application/vnd.github.v3+json'
            }
        });
        if (response.status >= 200 && 300 <= response.status) throw new Error();
        return response.json(); // parses JSON response into native JavaScript objects
    } catch (error) {
        setStatus('Failed to load the page! Retrying in 5 seconds...');
        setTimeout(() => {
            window.location = window.location;
        }, 5);
    }
}

function similarity(s1, s2) {
    var longer = s1;
    var shorter = s2;
    if (s1.length < s2.length) {
        longer = s2;
        shorter = s1;
    }
    var longerLength = longer.length;
    if (longerLength === 0) {
        return 1.0;
    }
    return (longerLength - editDistance(longer, shorter)) / parseFloat(longerLength);
}

function editDistance(s1, s2) {
    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();

    var costs = [];
    for (var i = 0; i <= s1.length; i++) {
        var lastValue = i;
        for (var j = 0; j <= s2.length; j++) {
            if (i === 0)
                costs[j] = j;
            else {
                if (j > 0) {
                    var newValue = costs[j - 1];
                    if (s1.charAt(i - 1) !== s2.charAt(j - 1))
                        newValue = Math.min(Math.min(newValue, lastValue),
                            costs[j]) + 1;
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
        }
        if (i > 0)
            costs[s2.length] = lastValue;
    }
    return costs[s2.length];
}