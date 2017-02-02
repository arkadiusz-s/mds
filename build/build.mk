##
#
#  Managed Data Structures
#  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Lesser General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Lesser General Public License for more details.
#
#  You should have received a copy of the GNU Lesser General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#  As an exception, the copyright holders of this Library grant you permission
#  to (i) compile an Application with the Library, and (ii) distribute the 
#  Application containing code generated by the Library and added to the 
#  Application during this compilation process under terms of your choice, 
#  provided you also meet the terms and conditions of the Application license.
#

######################
#
# This makefile assumes that it resides in the *build* directory at
# the top level of the project and that this directory contains
# build.mk (this file), defs.mk, project.mk, and one or more build
# configuration directories, each of which contains a makefile.  This
# project may be contained within another project.  If it is, it is
# assumed to be at the top level in that project.  The overall "git
# base" is the first ancestor that isn't a project and so doesn't have
# a build dir.
#
# Within the project dir, besides the build dir, there is assumed to
# be a src dir and an include dir.  It may optionally include a
# "tools" dir and a "tests" dir, which contain the (self-contained)
# source for binaries that don't form part of the library.
#
# make install identifies (or creates) an install dir in (by default)
# the git base dir.  It copies over the library, all .h files and
# identified subdirs (by default all of them) from include, and
# identified binaries from tools (by default none).
#
# The make is initiated from one of the config dirs.  The makefile
# there may set some configuration-related variables (e.g.,
# $(optlevel) or $(debuglevel)) and then includes project.mk.  This
# makefile first includes defs.mk, then sets project-related variables
# (e.g., the locations of source files and include files), and finally
# includes build.mk
#
######################


# The project's include directory

incl_dir := $(project_dir)/include

# The project's source directory

src_dir := $(project_dir)/src

$(project_name)_generated_src_dir ?= $(generated_src_dir)
generated_src_dir ?= $(project_dir)/generated-src


#######
#
# $(call subdirs,dir) lists all non-empty subdirectories of a directory
#
######

subdirs = $(foreach f,$(wildcard $(1)/*),$(if $(wildcard $(f)/*),$(f)))

# Non-empty subdirectories of $(src)

static_src_subdirs := $(call subdirs,$(src_dir))
generated_src_subdirs := $(call subdirs,$(generated_src_dir))

ignore_src_dirs ?= unused obsolete
$(project_name)_ignore_src_dirs ?= $(ignore_src_dirs)

ignore_src_dir_patterns := $(addprefix %/,$($(project_name)_ignore_src_dirs))

static_src_dirs := $(src_dir) $(filter-out $(ignore_src_dir_patterns),$(static_src_subdirs))
generated_src_dirs := $(generated_src_dir) $(filter-out $(ignore_src_dir_patterns),$(generated_src_subdirs))

src_dirs := $(static_src_dirs) $(generated_src_dirs)

static_src_files := $(wildcard $(addsuffix /*.cpp,$(static_src_dirs)))
generated_src_files := $(wildcard $(addsuffix /*.cpp,$(generated_src_dirs)))
rel_src_files := $(patsubst $(src_dir)/%,%,$(static_src_files)) $(patsubst $(generated_src_dir)/%,%,$(generated_src_files))
obj_root_dir := objs
obj_files := $(addprefix $(obj_root_dir)/,$(rel_src_files:.cpp=.o))
dep_root_dir := dependencies
dep_files := $(addprefix $(dep_root_dir)/,$(rel_src_files:.cpp=.d))

lib_dir := libs
make_static_lib = $(lib_dir)/lib$(1).a
make_shared_lib = $(lib_dir)/lib$(1).so

bin_classes := tools tests

bin_base_dir = $(project_dir)/$(1)
bin_dirs = $(wildcard $(call bin_base_dir,$(1))/*)
bin_names = $(patsubst $(call bin_base_dir,$(1))/%,%,$(call bin_dirs,$(1)))
bin_dir = $(1)
bin_bins = $(patsubst $(call bin_base_dir,$(1))/%,$(call bin_dir,$(1))/%,$(call bin_dirs,$(1)))
bin_src_files = $(wildcard $(addsuffix /*.cpp,$(call bin_base_dir,$(1))/$(2)))
bin_rel_src_files = $(patsubst $(project_dir)/%,%,$(call bin_src_files,$(1),$(2)))
bin_obj_files = $(addprefix $(obj_root_dir)/,$(patsubst %.cpp,%.o,$(call bin_rel_src_files,$(1),$(2))))
bin_dep_files = $(addprefix $(dep_root_dir)/,$(patsubst %.cpp,%.d,$(call bin_rel_src_files,$(1),$(2))))

# By default, the library name will be the name of the project.  If
# the project is "foo", the actual library will be "libfoo.a"

lib_name ?= $(project_name)
$(project_name)_lib_name ?= $(lib_name)

static_lib := $(call make_static_lib,$($(project_name)_lib_name))
shared_lib := $(call make_shared_lib,$($(project_name)_lib_name))

lib_targets ?= $(static_lib)
$(project_name)_lib_targets ?= $(lib_targets)

obj_to_dep = $(1:$(obj_root_dir)/%.o=$(dep_root_dir)/%.d)
not_defined = $(findstring $(origin $1),undefined)

projects_used ?=
$(project_name)_projects_used ?= $(projects_used)


######################################
# These are the variables you might want to override or modify in a
# config-specific makefile.


install_dir ?= $(git_base_dir)/install
$(project_name)_install_dir ?= $(install_dir)

install_includes_dir ?= $($(project_name)_install_dir)/include
$(project_name)_install_includes_dir ?= $(install_includes_dir)

install_lib_dir ?= $($(project_name)_install_dir)/lib
$(project_name)_install_lib_dir ?= $(install_lib_dir)

install_bin_dir ?= $($(project_name)_install_dir)/bin
$(project_name)_install_bin_dir ?= $(install_bin_dir)

install_doc_dir ?= $($(project_name)_install_dir)/doc
$(project_name)_install_doc_dir ?= $(install_doc_dir)

install_html_dir ?= $($(project_name)_install_doc_dir)/html
$(project_name)_install_html_dir ?= $(install_html_dir)

install_pdf_dir ?= $($(project_name)_install_doc_dir)/pdf
$(project_name)_install_pdf_dir ?= $(install_pdf_dir)


installed_include_subdirs ?= $(notdir $(call subdirs,$(incl_dir)))
$(project_name)_installed_include_subdirs ?= $(installed_include_subdirs)

installed_libs ?= $(notdir $(lib_targets))
$(project_name)_installed_libs ?= $(installed_libs)

installed_lib_files = $(addprefix $($(project_name)_install_lib_dir)/,$($(project_name)_installed_libs))

installed_progs ?= 
$(project_name)_installed_progs ?= $(installed_progs)

installed_prog_files = $(addprefix $($(project_name)_install_bin_dir)/,$($(project_name)_installed_progs))

installed_manual_name ?= $(project_name)-api.pdf
$(project_name)_installed_manual_name = $(installed_manual_name)

installed_manual ?= $($(project_name)_install_pdf_dir)/$($(project_name)_installed_manual_name)
$(project_name)_installed_manual ?= $(installed_manual)

installed_html ?= $($(project_name)_install_html_dir)/$(project_name)
$(project_name)_installed_html ?= $(installed_html)


incl_dirs ?= . $(incl_dir) $(foreach p,$($(project_name)_projects_used),$($(p)_include_dirs)) 
$(project_name)_incl_dirs ?= $(incl_dirs)

cpp_std ?= c++14
$(project_name)_cpp_std ?= $(cpp_std)

std_flag = $(if $(call not_defined,$(project_name)_cpp_std),,-std=$($(project_name)_cpp_std))

cpp_defines ?=
$(project_name)_cpp_defines ?= $(cpp_defines)

extra_incl_dirs ?=
$(project_name)_extra_incl_dirs ?= $(extra_incl_dirs)

cpp_includes ?= $(addprefix -I,$($(project_name)_incl_dirs) $($(project_name)_extra_incl_dirs))
$(project_name)_cpp_includes ?= $(cpp_includes)

cpp_dep_flags ?= -MMD -MP -MF"$(call obj_to_dep,$@)"
$(project_name)_cpp_dep_flags ?= $(cpp_dep_flags)

extra_cpp_flags ?=
$(project_name)_extra_cpp_flags ?= $(extra_cpp_flags)

optlevel ?= g
$(project_name)_optlevel ?= $(optlevel)

opt_flag ?= -O$($(project_name)_optlevel)
$(project_name)_opt_flag ?= $(opt_flag)

debuglevel ?= gdb3
$(project_name)_debuglevel ?= $(debuglevel)

debug_flag ?= -g$($(project_name)_debuglevel)
$(project_name)_debug_flag ?= $(debug_flag)

extra_cxx_flags ?= -Wall -fmessage-length=0 -pthread -fPIC
$(project_name)_extra_cxx_flags ?= $(extra_cxx_flags)

CPPFLAGS ?= \
	$(std_flag) \
	$($(project_name)_cpp_defines) \
	$($(project_name)_cpp_includes) \
	$($(project_name)_cpp_dep_flags) \
	$($(project_name)_extra_cpp_flags)
$(project_name)_CPPFLAGS ?= $(CPPFLAGS)

CXXFLAGS ?= \
	$($(project_name)_opt_flag) \
	$($(project_name)_debug_flag) \
	$($(project_name)_extra_cxx_flags)
$(project_name)_CXXFLAGS ?= $(CXXFLAGS)

LDFLAGS ?=
$(project_name)_LDFLAGS ?= $(LDFLAGS)

LIBS ?= -lstdc++ -lpthread
$(project_name)_LIBS ?= $(LIBS)

######################################


.DEFAULT_GOAL := all

all: $(lib_targets) tools

.DELETE_ON_ERROR:

.PHONY: FORCE
FORCE:

define used_projects_fn

$(1)_config ?= $$(config)
$(1)_project_dir ?= $$(if $$(wildcard $$(project_dir)/$(1)/build),$$(project_dir)/$(1),$$(git_base_dir)/$(1))
$(1)_build_dir ?= $$($(1)_project_dir)/build/$$($(1)_config)
$(1)_include_dirs ?= $$($(1)_project_dir)/include
$(1)_lib_name ?= lib$(1).a
$(1)_lib_target = libs/$$($(1)_lib_name)
$(1)_lib ?= $$($(1)_build_dir)/$$($(1)_lib_target)

$$($(1)_lib): FORCE
	$$(MAKE) -C $$($(1)_build_dir) $$($(1)_lib_target)

clean-$(1): FORCE
	$$(MAKE) -C $$($(1)_build_dir) clean-recursive

install-$(1): FORCE
	$$(MAKE) -C $$($(1)_build_dir) install

install-recursive-$(1): FORCE
	$$(MAKE) -C $$($(1)_build_dir) install-recursive

private-doc-config-$(1): FORCE
	$$(MAKE) -C $$($(1)_build_dir) private-doxygen.config

public-doc-config-$(1): FORCE
	$$(MAKE) -C $$($(1)_build_dir) public-doxygen.config

endef

$(foreach p,$($(project_name)_projects_used),$(eval $(call used_projects_fn,$(p))))

libs_used = $(foreach p,$($(project_name)_projects_used),$($(p)_lib))

define bin_rule

.PHONY: clean-$(2)

clean-$(2):
	-rm -rf $(call bin_dir,$(1)/$(2)) $(obj_root_dir)/$(call bin_dir,$(1)/$(2)) $(dep_root_dir)/$(call bin_dir,$(1)/$(2))

$(2)_extra_libs ?=
$(2)_cpp_std ?= $$($(project_name)_cpp_std)
$(2)_std_flag = $$(if $$(call not_defined,$(2)_cpp_std),,-std=$$($(2)_cpp_std))
$(2)_cpp_defines ?= $$($(project_name)_cpp_defines)
$(2)_cpp_extra_includes ?=
$(2)_cpp_includes ?= $$($(2)_cpp_extra_includes) $$($(project_name)_cpp_includes)
$(2)_cpp_dep_flags ?= $$($(project_name)_cpp_dep_flags)
$(2)_extra_cpp_flags ?= $$($(project_name)_extra_cpp_flags)
$(2)_optlevel ?= $$($(project_name)_optlevel)
$(2)_opt_flag ?= -O$$($(2)_optlevel)
$(2)_debuglevel ?= $$($(project_name)_debuglevel)
$(2)_debug_flag ?= -g$$($(2)_debuglevel)
$(2)_extra_cxx_flags ?= $$($(project_name)_extra_cxx_flags)

$(2)_CPPFLAGS ?= \
	$$($(2)_std_flag) \
	$$($(2)_cpp_defines) \
	$$($(2)_cpp_includes) \
	$$($(2)_cpp_dep_flags) \
	$$($(2)_extra_cpp_flags)

$(2)_CXXFLAGS ?= \
	$$($(2)_opt_flag) \
	$$($(2)_debug_flag) \
	$$($(2)_extra_cxx_flags)

$(2)_LDFLAGS ?= $$($(project_name)_LDFLAGS)
$(2)_LIBS ?= $$($(project_name)_LIBS) $$($(2)_extra_libs)


ifeq ($(strip $(call bin_src_files,$(1),$(2))),)
$(call bin_dir,$(1))/$(2):
else
$(call bin_dir,$(1))/$(2): $(call bin_obj_files,$(1),$(2)) $$(static_lib) $$(libs_used) 
	@echo Building $$@
	-@mkdir -p $$(dir $$@)
	$$(CXX) $$($(2)_LDFLAGS) -o $$@ $$^ $$($(2)_LIBS)	
	@echo Finished building $$@
	@echo
endif

$$(obj_root_dir)/$(1)/$(2)/%.o: $$(project_dir)/$(1)/$(2)/%.cpp
	@echo Building file: $$<
	-@mkdir -p $$(dir $$@)
	-@mkdir -p $$(dir $$(call obj_to_dep,$$@))
	$$(CXX) -c $$($(2)_CPPFLAGS) $$($(2)_CXXFLAGS) -o "$$@" "$$<"
	@echo Finished building: $$<
	@echo

$($(project_name)_install_bin_dir)/$(2): $(call bin_dir,$(1))/$(2)
	@echo Installing $$^ as $$@
	-@mkdir -p $$(dir $$@)
	cp $$^ $($(project_name)_install_bin_dir)

install-$(2): $($(project_name)_install_bin_dir)/$(2)
endef

define bin_class_rule

.PHONY: $(1) clean-$(1) clean-$(1)-binaries

$(1): $(call bin_bins,$(1))

clean-$(1): $(foreach i,$(call bin_names,$(1)),clean-$(i))

clean: clean-$(1)

clean-$(1)-binaries:
	-rm -f $(call bin_dir,$(1))/*


$(foreach i,$(call bin_names,$(1)),$(call bin_rule,$(1),$(i)))
$(foreach i,$(call bin_names,$(1)),-include $(call bin_dep_files,$(1),$(i)))


endef

#$(info $(call bin_class_rule,tools))
#$(info $(call bin_class_rule,tests))

-include $(dep_files)

$(foreach p,$(bin_classes),$(eval $(call bin_class_rule,$(p))))

$(static_lib) : $(obj_files) $(libs_used)
	@echo Building library: $@
	-@mkdir -p $(dir $@)
	$(AR) $(ARFLAGS) $@ $?
	@echo Finished building library: $@
	@echo

$(shared_lib) : $(obj_files) $(libs_used)
	@echo Building library: $@
	-@mkdir -p $(dir $@)
	$(CXX) $(LDFLAGS) -shared -o $@ $^ $(LIBS)
	@echo Finished building library: $@
	@echo


$(obj_root_dir)/%.o: $(src_dir)/%.cpp
	@echo Building file: $<
	-@mkdir -p $(dir $@)
	-@mkdir -p $(dir $(call obj_to_dep,$@))
	$(CXX) -c $($(project_name)_CPPFLAGS) $($(project_name)_CXXFLAGS) -o "$@" "$<"
	@echo Finished building: $<
	@echo

$(obj_root_dir)/%.o: $(generated_src_dir)/%.cpp
	@echo Building file: $<
	-@mkdir -p $(dir $@)
	-@mkdir -p $(dir $(call obj_to_dep,$@))
	$(CXX) -c $($(project_name)_CPPFLAGS) $($(project_name)_CXXFLAGS) -o "$@" "$<"
	@echo Finished building: $<
	@echo

define default_doxygen_args :=
RECURSIVE = YES
JAVADOC_AUTOBRIEF = YES
endef

doxygen_args ?=
doc_dir ?= $(project_dir)/doc
doc_latex_dir ?= $(doc_dir)/latex
manual ?= $(doc_dir)/$(project_name)-api.pdf
doxygen_private_config ?= private-doxygen.config
doxygen_public_config ?= public-doxygen.config
DOXYGEN ?= doxygen

define doxygen_common_args =
$(default_doxygen_args)
$(doxygen_args)
OUTPUT_DIRECTORY = $(doc_dir)
endef

define doxygen_private_args =
$(foreach p,$($(project_name)_projects_used),
@INCLUDE = $($(p)_build_dir)/private-doxygen.config)
$(doxygen_common_args)
EXTRACT_PRIVATE = YES
INPUT += $(src_dir) $(incl_dir)
endef

all_include_subdirs = $(notdir $(call subdirs,$(incl_dir)))


define doxygen_public_args =
$(foreach p,$($(project_name)_projects_used),
@INCLUDE = $($(p)_build_dir)/public-doxygen.config)
$(doxygen_common_args)
EXTRACT_PRIVATE = NO
INPUT += $(incl_dir)
EXCLUDE += $(addprefix $(incl_dir)/,$(filter-out $($(project_name)_installed_include_subdirs),$(all_include_subdirs)))
endef

.PHONY: doc manual

$(doxygen_private_config): FORCE $(foreach p,$($(project_name)_projects_used),private-doc-config-$(p))
	$(file >$@,$(doxygen_private_args))

$(doxygen_public_config): FORCE $(foreach p,$($(project_name)_projects_used),public-doc-config-$(p))
	$(file >$@,$(doxygen_public_args))

public-doc: $(doxygen_public_config)
	$(DOXYGEN) $(doxygen_public_config)

private-doc: $(doxygen_private_config)
	$(DOXYGEN) $(doxygen_private_config)

public-manual:	public-doc
	$(MAKE) -C $(doc_latex_dir) refman.pdf
	cp $(doc_latex_dir)/refman.pdf $(manual)

private-manual:	private-doc
	$(MAKE) -C $(doc_latex_dir) refman.pdf
	cp $(doc_latex_dir)/refman.pdf $(manual)

.PHONY: clean clean-lib clean-objs clean-dependencies clean-recursive

clean: clean-lib clean-objs clean-dependencies clean-generated

clean-objs:
	-rm -rf $(obj_root_dir)/*

clean-dependencies:
	-rm -rf $(dep_root_dir)/*

clean-lib:
	-rm -f $(lib_dir)/*

clean-generated:
	-rm -f $(generated_src_dir)/*

clean-recursive: clean $(foreach p,$($(project_name)_projects_used),clean-$(p))

clean-doc: clean-manual clean-html

clean-manual:
	-rm $(manual)

clean-html:
	-rm -r $(doc_dir)/html

.PHONY: install install-recursive install-libs install-progs install-includes
.PHONY: install-html install-manual install-doc

install: install-libs install-progs install-includes

install-recursive: install $(foreach p,$($(project_name)_projects_used),install-recursive-$(p))

install-libs: $(installed_lib_files)

install-progs: $(foreach i,$($(project_name)_installed_progs),install-$(i))

install-includes: FORCE
	@echo Syncing include dirs
	$(if $(wildcard $(incl_dir)/*.h),cp $(incl_dir)/*.h $($(project_name)_install_includes_dir))
	$(if $(addprefix $(incl_dir)/,$($(project_name)_installed_include_subdirs)),\
	rsync -rvui --include '*/' --include '*.h' --exclude '*' --delete-excluded \
	  $(addprefix $(incl_dir)/,$($(project_name)_installed_include_subdirs)) $($(project_name)_install_includes_dir))

install-doc: install-html install-manual

install-manual: public-manual
	-@mkdir -p $(dir $($(project_name)_installed_manual))
	cp $(manual) $($(project_name)_installed_manual)

install-html: public-doc
	-@mkdir -p $($(project_name)_installed_html)
	(cd $(doc_dir)/html; rsync -rvui . $($(project_name)_installed_html))

$($(project_name)_install_lib_dir)/%: $(lib_dir)/%
	-@mkdir -p $($(project_name)_install_lib_dir)
	@echo Installing $< to $($(project_name)_install_lib_dir)
	cp $< $($(project_name)_install_lib_dir)

$($(project_name)_install_bin_dir)/%: $(lib_dir)/%
	-@mkdir -p $($(project_name)_install_lib_dir)
	@echo Installing $< to $($(project_name)_install_lib_dir)
	cp $< $($(project_name)_install_lib_dir)

.PHONY: clean-install clean-installed-libs clean-installed-includes clean-installed-progs
.PHONY: clean-installed-html clean-installed-manual clean-installed-doc

clean-install: clean-installed-libs clean-installed-progs clean-installed-includes

clean-installed-libs: 
	-rm $(installed_lib_files)

clean-installed-includes: 
	-rm -r $(addprefix $($(project_name)_install_includes_dir)/,$($(project_name)_installed_include_subdirs) $(notdir $(wildcard $(incl_dir)/*.h)))

clean-installed-progs:
	-rm -r $(installed_prog_files)

clean-installed-doc: clean-installed-manual clean-installed-html

clean-installed-manual:
	-rm $($(project_name)_installed_manual)

clean-installed-html:
	-rm -r $($(project_name)_installed_html)

####
#
# This is an internal HPE target that allows us to easily distribute
# the autobuild makefiles and documentation to all projects that use
# it.
#
####

MDS_CRD := Managed Data Structures
MPGC_CRD := Multi Process Garbage Collector

autobuild_project_dirs ?= gc:MPGC common:MPGC core:MDS java-api:MDS

define dist_copy_file
-sed '$(3)s/.*/$(4)/' $(build_dir)/$(1) >$(git_base_dir)/$(2)/build/$(notdir $(1))
endef
define distribute_to
distribute-to-$(1): FORCE
ifneq ($$(build_dir),$$(git_base_dir)/$(1)/build)
	$(call dist_copy_file,build.mk,$(1),3,#  $(2))
	$(call dist_copy_file,defs.mk,$(1),3,#  $(2))
	$(call dist_copy_file,BUILDING.md,$(1),2,      $(2))
endif
endef

nullstring :=
space := $(nullstring) $(nullstring)
auto_build_dir_name = $(word 1,$(subst :,$(space),$(1)))
auto_build_crd = $($(word 2,$(subst :,$(space),$(1)))_CRD)

define dist_to_aux
$(eval $(call distribute_to,$(call auto_build_dir_name,$(1)),$(call auto_build_crd,$(1))))
endef

$(foreach p,$(autobuild_project_dirs),$(eval $(call dist_to_aux,$(p))))

distribute-autobuild: $(foreach p,$(autobuild_project_dirs),distribute-to-$(call auto_build_dir_name,$(p)))


