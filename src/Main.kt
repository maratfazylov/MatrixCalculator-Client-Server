import common.IMatrixServer
import common.models.*
import java.util.Scanner
import javax.swing.SwingUtilities

fun main() {
    val server: IMatrixServer = MatrixServerProxy()
    
    SwingUtilities.invokeLater {
        MainWindow(server).isVisible = true
    }
}