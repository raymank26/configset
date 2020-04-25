package com.letsconfig.dashboard

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.DefaultWindowManager
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextBox
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal


object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        // Setup terminal and screen layers

        // Setup terminal and screen layers
        val terminal: Terminal = DefaultTerminalFactory().setForceTextTerminal(true).createTerminal()
        val screen: Screen = TerminalScreen(terminal)
        screen.startScreen()

        // Create panel to hold components

        // Create panel to hold components
        val panel = Panel()
        panel.setLayoutManager(GridLayout(2))

        panel.addComponent(Label("Forename"))
        panel.addComponent(TextBox())

        panel.addComponent(Label("Surname"))
        panel.addComponent(TextBox())

        panel.addComponent(EmptySpace(TerminalSize(0, 0))) // Empty space underneath labels

        panel.addComponent(Button("Submit"))

        // Create window to hold the panel

        // Create window to hold the panel
        val window = BasicWindow()
        window.component = panel

        // Create gui and start gui

        // Create gui and start gui
        val gui = MultiWindowTextGUI(screen, DefaultWindowManager(), EmptySpace(TextColor.ANSI.BLUE))
        gui.addWindowAndWait(window)
    }
}