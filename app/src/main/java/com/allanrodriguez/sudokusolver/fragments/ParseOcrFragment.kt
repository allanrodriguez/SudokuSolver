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
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable

class ParseOcrFragment : Fragment() {

    private val trainedData = "eng.traineddata"

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        val image: Serializable? = arguments?.getSerializable(IMAGE)
        val imageRect: Rect? = arguments?.getParcelable(IMAGE_RECT)
        val squareRect: Rect? = arguments?.getParcelable(SQUARE_RECT)
        val tessData = File(File(context?.cacheDir, "tessdata"), trainedData)

        if (image is File && imageRect != null && squareRect != null) {
            CoroutineScope(Dispatchers.Default).launch {
                copyTessDataToCache(tessData)
                val puzzle: Array<IntArray> =
                        parseSudokuFromImage(cropImage(image, imageRect, squareRect), tessData.parentFile.absolutePath)
                tessData.delete()

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

    private fun copyTessDataToCache(tessData: File) {
        val tessDataParent: File = tessData.parentFile

        Log.d(TAG, "Creating folder ${tessDataParent.absolutePath}...")
        if (!(tessDataParent.mkdirs() || tessDataParent.exists())) {
            Log.e(TAG, "tessdata directory was not created!")
        }
        Log.d(TAG, "tessdata directory created.")

        val buffer = ByteArray(8192)

        context?.assets?.open(trainedData)?.use { source ->
            FileOutputStream(tessData).use { target ->
                while (source.read(buffer) > 0) {
                    target.write(buffer)
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

        val x: Int = (scalingFactor * squareRect.left).toInt() - 4
        val y: Int = (scalingFactor * squareRect.top).toInt() - 4
        val width: Int = (scalingFactor * squareRect.width()).toInt() + 4
        val height: Int = (scalingFactor * squareRect.height()).toInt() + 4

        Log.d(TAG, "Bitmap dimensions are ($x, $y, $width, $height)")

        return Bitmap.createBitmap(originalImage, x, y, width, height)
    }

    private fun setOcrResultToPuzzle(sudoku: Array<IntArray>) {
        Log.i(tag, "Copying parsed puzzle to the enter puzzle view-model...")

        val puzzleFragment: Fragment =
                fragmentManager?.findFragmentByTag(EnterPuzzleFragment::class.java.simpleName) as Fragment
        val puzzleViewModel: EnterPuzzleViewModel =
                ViewModelProviders.of(puzzleFragment).get(EnterPuzzleViewModel::class.java)

        for (i: Int in 0..8) {
            for (j: Int in 0..8) {
                puzzleViewModel.sudoku[i][j].apply {
                    wasValueSetByUser.value = false
                    cellValue.value = if (sudoku[i][j] > 0) sudoku[i][j].toString() else ""
                }
            }
        }

        Log.i(tag, "Done copying parsed puzzle to the enter puzzle view-model.")
    }

    private external fun parseSudokuFromImage(image: Bitmap, pathToTrainedData: String): Array<IntArray>

    companion object {
        const val TAG: String = "ParseOcrFragment"

        private const val IMAGE: String = "IMAGE"
        private const val IMAGE_RECT: String = "IMAGE_RECT"
        private const val SQUARE_RECT: String = "SQUARE_RECT"

        init {
            System.loadLibrary("parseocrjni")
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
