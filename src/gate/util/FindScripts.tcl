# FindScripts.tcl
#
# lists all the .tcl scripts in the gate/src directory
#
# Hamish, 14/3/00 
# $Id: FindScripts.tcl 820 2001-01-30 14:18:02Z hamish $


namespace eval GATE {

  set tclFiles [list]

  proc filter { dir } {
    global tclFiles

    foreach f [glob -nocomplain ${dir}/*] {
      if [file isdirectory $f] { filter $f }
      if [string match {*.tcl} $f] { lappend tclFiles [string range $f 2 end] }
    }
  }

  proc findScripts { } {
    global tclFiles

    # cd to the gate/src directory
    # assumes that we are in gate or one of its subdirectories when we start
    set WD [pwd]
    if { ! [string match "*gate*" $WD] } { error "not in the gate directories" }
    while { [file tail $WD] != "gate" } { cd ..; set WD [pwd] }
    cd src

    filter {.}

    set tclFiles
  }

} ;# namespace eval GATE
