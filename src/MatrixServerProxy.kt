import common.IMatrixServer
import common.models.MatrixModel
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket

class MatrixServerProxy(
    private val host: String = "localhost",
    private val port: Int = 8080
) : IMatrixServer {
    private var socket: Socket? = null
    private var input: ObjectInputStream? = null
    private var output: ObjectOutputStream? = null

    init {
        connect()
    }

    private fun connect() {
        socket = Socket(host, port)
        output = ObjectOutputStream(socket?.getOutputStream())
        input = ObjectInputStream(socket?.getInputStream())
    }

    override fun getAllMatrices(): List<MatrixModel> {
        output?.writeObject("GET_ALL")
        return input?.readObject() as List<MatrixModel>
    }

    override fun getMatrix(id: Int): MatrixModel? {
        output?.writeObject("GET_BY_ID")
        output?.writeObject(id)
        return input?.readObject() as? MatrixModel
    }

    override fun close() {
        try {
            output?.writeObject("EXIT")
            socket?.close()
            input?.close()
            output?.close()
        } catch (e: Exception) {
            println("Ошибка при закрытии соединения: ${e.message}")
        }
    }

    override fun saveMatrix(matrix: MatrixModel): Boolean {
        output?.writeObject("SAVE")
        output?.writeObject(matrix)
        return input?.readObject() as Boolean
    }
} 