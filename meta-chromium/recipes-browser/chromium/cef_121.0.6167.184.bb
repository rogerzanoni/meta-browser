require common-gn.inc

REQUIRED_DISTRO_FEATURES = "wayland"

DEPENDS += "\
        at-spi2-atk \
        virtual/egl \
        wayland \
        wayland-native \
"

RDEPENDS:${PN} = "bash"
RDEPENDS:${PN}-dev = "bash"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/cef:"

# chromium args
GN_ARGS += "\
        ${PACKAGECONFIG_CONFARGS} \
        use_ozone=true \
        use_egl=true \
        use_wayland_gbm=false \
        ozone_auto_platforms=false \
        ozone_platform_headless=true \
        ozone_platform_wayland=true \
        ozone_platform_x11=false \
        system_wayland_scanner_path="${STAGING_BINDIR_NATIVE}/wayland-scanner" \
        use_system_wayland_scanner=true \
        use_xkbcommon=true \
        use_system_minigbm=true \
        use_system_libdrm=true \
        use_system_libffi=true \
        use_gtk=false \
"

# cef-specific flags
GN_ARGS += "\
        cef_use_gtk=false \
        is_ozone_x11=false \
        use_sysroot=false \
"

BRANCH = "6167"
SRCREV_cef = "4d3b0b471a5e15a0de692cdfe8a65f0cefcb4228"
SRCREV_depot_tools = "6444de14d1af435a425d9c6306c620c77fff9191"
SRCREV_FORMAT = "_ef_depot-tools"
CHROMIUM_DIR = "${WORKDIR}/chromium-${PV}"
CEF_DIR = "${CHROMIUM_DIR}/cef"
CEF_INSTALL_DIR = "/opt/cef"
DEPOT_TOOLS_DIR="${WORKDIR}/depot_tools"

S = "${CHROMIUM_DIR}"

OUTPUT_DIR = "out/Release_GN_${GN_TARGET_ARCH_NAME}"
B = "${S}/${OUTPUT_DIR}"

SRC_URI:append = "\
    git://bitbucket.org/chromiumembedded/cef.git;branch=${BRANCH};protocol=https;rev=${SRCREV_cef};name=cef;destsuffix=chromium-${PV}/cef \
    file://0001-Make-patcher-work-outside-a-git-checkout.patch;patchdir=cef \
    file://0002-Fix-wayland-ozone-build.patch;patchdir=cef \
    file://0003-Remove-X11-from-the-list-of-standard-libs.patch;patchdir=cef \
    \
    git://chromium.googlesource.com/chromium/tools/depot_tools.git;protocol=https;rev=${SRCREV_depot_tools};name=depot-tools;destsuffix=depot_tools;branch=main"

do_configure[network] = "1"

do_configure() {
    export DEPOT_TOOLS_UPDATE=0
    export GCLIENT_PY3=1
    export DEPOT_TOOLS_BOOTSTRAP_PYTHON3=0
    export GN_DEFINES="${GN_ARGS}"
    export PATH="${DEPOT_TOOLS_DIR}:$PATH"

    cd ${DEPOT_TOOLS_DIR}
    ./ensure_bootstrap

    # Download a few dependencies.  Check the current chromium DEPS file when upgrading to a new milestone.
    cd ${S}
    vpython3 third_party/depot_tools/download_from_google_storage.py --no_resume --extract --no_auth --bucket chromium-fonts -s third_party/test_fonts/test_fonts.tar.gz.sha1

    python3 ./build/linux/unbundle/replace_gn_files.py --system-libraries ${GN_UNBUNDLE_LIBS}

    cd ${S}/cef
    python3 tools/gclient_hook.py
}

do_compile[progress] = "outof:^\[(\d+)/(\d+)\]\s+"

do_compile() {
    export PATH="${DEPOT_TOOLS_DIR}:${S}/third_party/ninja:$PATH"
    ninja ${PARALLEL_MAKE} -C ${B} libcef chrome_sandbox
}

do_install() {
    export DEPOT_TOOLS_UPDATE=0
    export GCLIENT_PY3=1
    export PATH="${DEPOT_TOOLS_DIR}:$PATH"
    export GN_DEFINES="${GN_ARGS}"

    cd ${S}/cef
    python3 tools/make_distrib.py --output-dir ${B}/dist \
                                  --distrib-subdir cef \
                                  --distrib-subdir-suffix ozone \
                                  --no-docs \
                                  --no-symbols \
                                  --no-archive \
                                  --allow-partial \
                                  --ninja-build \
                                  --${GN_TARGET_ARCH_NAME}-build \
                                  --ozone

    install -d ${D}${CEF_INSTALL_DIR}

    cp -R --no-dereference --preserve=mode,links -v ${B}/dist/cef_ozone/* ${D}${CEF_INSTALL_DIR}

    install -m 0755 ${S}/chrome/installer/linux/common/wrapper  ${D}${CEF_INSTALL_DIR}
}

SYSROOT_DIRS:prepend = "/opt "

FILES:${PN} += " \
    ${CEF_INSTALL_DIR}/LICENSE.txt \
    ${CEF_INSTALL_DIR}/README.txt \
    ${CEF_INSTALL_DIR}/Release \
    ${CEF_INSTALL_DIR}/Resources \
"

FILES:${PN}-dev += " \
    ${CEF_INSTALL_DIR}/Doxyfile \
    ${CEF_INSTALL_DIR}/cef_paths.gypi \
    ${CEF_INSTALL_DIR}/cef_paths2.gypi \
    ${CEF_INSTALL_DIR}/CMakeLists.txt \
    ${CEF_INSTALL_DIR}/cmake \
    ${CEF_INSTALL_DIR}/include \
    ${CEF_INSTALL_DIR}/libcef_dll \
    ${CEF_INSTALL_DIR}/tests \
    ${CEF_INSTALL_DIR}/README.md \
    ${CEF_INSTALL_DIR}/wrapper \
"
