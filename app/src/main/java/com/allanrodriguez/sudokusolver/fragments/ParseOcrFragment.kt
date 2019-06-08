package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class ParseOcrFragment : Fragment() {

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        val image: Serializable? = arguments?.getSerializable(IMAGE)
        val imageRect: Rect? = arguments?.getParcelable(IMAGE_RECT)
        val squareRect: Rect? = arguments?.getParcelable(SQUARE_RECT)
        val trainData = File(File(context?.cacheDir, "opencv"), TRAIN_DATA)

        if (image is File && imageRect != null && squareRect != null) {
            CoroutineScope(Dispatchers.Default).launch {
                copyTrainDataToCache(trainData)
                val puzzle = IntArray(81)
                parseSudokuFromImage(cropImage(image, imageRect, squareRect), trainData.absolutePath, puzzle)
                trainData.delete()

                withContext(Dispatchers.Main) {
                    setOcrResultToPuzzle(puzzle)
                    closeDialog()
                }
            }
        } else {
            Log.e(TAG, "An error occurred parsing the arguments passed to the Fragment.")
            closeDialog()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parse_ocr, container, false)
    }

    private fun closeDialog() {
        fragmentManager?.popBackStack()
    }

    private fun copyTrainDataToCache(trainData: File) {
        val parent: File = trainData.parentFile

        Log.d(TAG, "Creating folder ${parent.absolutePath}...")
        if (!(parent.mkdirs() || parent.exists())) {
            Log.e(TAG, "opencv directory was not created!")
        }
        Log.d(TAG, "opencv directory created.")

        val buffer = ByteArray(8192)

        context?.assets?.open(TRAIN_DATA)?.use { source ->
            FileOutputStream(trainData).use { target ->
                var bytesRead: Int = source.read(buffer)
                while (bytesRead > 0) {
                    target.write(buffer, 0, bytesRead)
                    bytesRead = source.read(buffer)
                }
            }
        }
    }

    private fun cropImage(image: File, imageRect: Rect, squareRect: Rect): Bitmap {
        val originalImage: Bitmap = BitmapFactory.decodeFile(image.absolutePath)
        val scalingFactor: Double = arrayOf(
                originalImage.width.toDouble() / imageRect.width(),
                originalImage.height.toDouble() / imageRect.height()
        ).average()

        val x: Int = (scalingFactor * squareRect.left).toInt()
        val y: Int = (scalingFactor * squareRect.top).toInt()
        val width: Int = (scalingFactor * squareRect.width()).toInt()
        val height: Int = (scalingFactor * squareRect.height()).toInt()

        Log.d(TAG, "Bitmap dimensions are ($x, $y, $width, $height)")

        return Bitmap.createBitmap(originalImage, x, y, width, height)
    }

    private fun setOcrResultToPuzzle(sudoku: IntArray) {
        Log.i(tag, "Copying parsed puzzle to the enter puzzle view-model...")

        val puzzleFragment: Fragment =
                fragmentManager?.findFragmentByTag(EnterPuzzleFragment::class.java.simpleName) as Fragment
        val puzzleViewModel: EnterPuzzleViewModel =
                ViewModelProviders.of(puzzleFragment).get(EnterPuzzleViewModel::class.java)

        for (i: Int in 0 until 81) {
            puzzleViewModel.sudoku[i / 9][i % 9].apply {
                wasValueSetByUser.value = false
                cellValue.value = if (sudoku[i] > 0) sudoku[i].toString() else ""
            }
        }

        Log.i(tag, "Done copying parsed puzzle to the enter puzzle view-model.")
    }

    private external fun parseSudokuFromImage(image: Bitmap, pathToTrainData: String, sudoku: IntArray)

    companion object {
        const val TAG: String = "ParseOcrFragment"

        private const val IMAGE: String = "IMAGE"
        private const val IMAGE_RECT: String = "IMAGE_RECT"
        private const val SQUARE_RECT: String = "SQUARE_RECT"

        private const val TRAIN_DATA = "digits_svm.yml"

        init {
            System.loadLibrary("sudoku_ocr")
        }

        @JvmStatic
        fun newInstance(image: File, imageRect: Rect, squareRect: Rect): ParseOcrFragment {
            return ParseOcrFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(IMAGE, image)
                    putParcelable(IMAGE_RECT, imageRect)
                    putParcelable(SQUARE_RECT, squareRect)
                }
            }
        }
    }
}
