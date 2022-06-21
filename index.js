const { parse } = require('java-parser');
const fs = require('fs');

let paths = JSON.parse(fs.readFileSync('paths.json'));

let sortedVersions = [
    '1_8_R1',
    '1_8_R2',
    '1_8_R3',
    '1_9_R1',
    '1_9_R2',
    '1_10_R1',
    '1_11_R1',
    '1_12_R1',
    '1_13_R1',
    '1_13_R2',
    '1_14_R1',
    '1_15_R1',
    '1_16_R1',
    '1_16_R2',
    '1_16_R3',
    '1_17_R1',
    '1_18_R1',
    '1_18_R2',
    '1_19_R1'
    ]

let packages = []

let newPaths = {};

for (let className in paths) {
    let versionArray = paths[className];
    let packageName = className.substring(0, className.lastIndexOf('/'));
    let packageIndex = packages.indexOf(packageName);
    if (packageIndex < 0) {
        packageIndex = packages.push(packageName) - 1;
    }
    let newVersion = [];
    for (let versionIndex in versionArray) {
        let version = versionArray[versionIndex];
        let index = sortedVersions.indexOf(version);
        if (index < 0) {
            throw (className+":"+version);
        }
        newVersion.push(index);
    }
    if (!newPaths[packageIndex]) newPaths[packageIndex] = [];
    newPaths[packageIndex].push({
        n: className.substring(className.lastIndexOf('/') + 1, className.length - 5),
        v: newVersion
    });
    // for (let version in paths[className]) {
    //     console.log(paths[className]);
    //     console.log(paths[className][version]);
    //     let index = sortedVersions.indexOf(paths[className][version]);
    //     if (index < 0) console.log(className+":"+paths[className][version]);
    //     paths[className] = index;
    // }
}

let newData = {
    sortedVersions: sortedVersions,
    packages: packages,
    paths: newPaths
}

fs.writeFileSync('docs/paths.json', JSON.stringify(newData));

console.log(JSON.stringify(newData));

