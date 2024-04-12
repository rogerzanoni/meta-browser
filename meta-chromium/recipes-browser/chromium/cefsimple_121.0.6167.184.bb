LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "cef"
RDEPENDS:${PN} = "bash"

BRANCH = "6167"
SRCREV = "4d3b0b471a5e15a0de692cdfe8a65f0cefcb4228"

SRC_URI = "git://bitbucket.org/chromiumembedded/cef.git;branch=${BRANCH};protocol=https;rev=${SRCREV}"
S = "${WORKDIR}/git/tests/cefsimple"

inherit cmake

EXTRA_OECMAKE = "-DCEF_ROOT=${RECIPE_SYSROOT}/opt/cef"

APP_INSTALL_DIR = "${bindir}/cef/cefapp-cefsimple"
CEF_RELEASE_DIR = "/opt/cef/Release"
CEF_RESOURCES_DIR = "/opt/cef/Resources"

do_copy_cef_files() {
    cp ${RECIPE_SYSROOT}/opt/cef/tests/cefsimple/CMakeLists.txt ${S}
    sed -i '1iset(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CEF_ROOT}/cmake")\nfind_package(CEF REQUIRED)\nadd_subdirectory(${CEF_ROOT}/libcef_dll libcef_dll_wrapper)
' ${S}/CMakeLists.txt
}
addtask copy_cef_files after do_prepare_recipe_sysroot before do_configure

CHROMIUM_EXTRA_ARGS = "--use-gl=egl --ozone-platform=wayland --enable-logging=stderr --v=2"

do_install() {
    install -d ${D}${APP_INSTALL_DIR}
    install -d ${D}${CEF_RELEASE_DIR}
    install -d ${D}${CEF_RESOURCES_DIR}

    install -v -D -m 755 ${B}/Release/cefsimple ${D}${APP_INSTALL_DIR}

    for f in ${RECIPE_SYSROOT}/${CEF_RELEASE_DIR}/*; do 
        if [[ "$(basename $f)" == 'Resources' ||  "$(basename $f)" == 'Release']]; then
            continue
        fi
        ln -srf ${D}${CEF_RELEASE_DIR}/$(basename $f) ${D}${APP_INSTALL_DIR}/$(basename $f)
        ln -srf ${D}${CEF_RELEASE_DIR}/libEGL.so ${D}${APP_INSTALL_DIR}/libEGL.so.1
        ln -srf ${D}${CEF_RELEASE_DIR}/libGLESv2.so ${D}${APP_INSTALL_DIR}/libGLESv2.so.2;
    done

    for f in ${RECIPE_SYSROOT}/${CEF_RESOURCES_DIR}/*; do 
        ln -srf ${D}${CEF_RESOURCES_DIR}/$(basename $f) ${D}${APP_INSTALL_DIR}/$(basename $f);
    done

    WRAPPER_FILE=${RECIPE_SYSROOT}/opt/cef/wrapper
    sed -e "s,@@CHANNEL@@,stable,g" \
        -e "s,@@PROGNAME@@,cefsimple,g" \
        ${WRAPPER_FILE} > cef-wrapper
	  install -m 0755 cef-wrapper ${D}${APP_INSTALL_DIR}
	  ln -srf ${D}${APP_INSTALL_DIR}/cef-wrapper ${D}${bindir}/cefsimple
    sed -i "s/^CHROME_EXTRA_ARGS=\"\"/CHROME_EXTRA_ARGS=\"${CHROMIUM_EXTRA_ARGS}\"/" ${D}${bindir}/cefsimple

    sed -i '1iEGL_LOG_LEVEL=debug\nLIBGL_DEBUG=verbose\nWAYLAND_DEBUG=1\nMESA_DEBUG=1\n' ${D}${bindir}/cefsimple
}

FILES:${PN} += " \
    ${APP_INSTALL_DIR} \
    ${CEF_RELEASE_DIR} \
    ${CEF_RESOURCES_DIR} \
"

INSANE_SKIP:${PN} += "dev-so libdir"
