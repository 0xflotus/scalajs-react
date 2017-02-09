package japgolly.scalajs.react.component

import scalajs.js
import japgolly.scalajs.react.internal._
import japgolly.scalajs.react.{Callback, ChildrenArg, CtorType, Key, PropsChildren, vdom, raw => Raw}


object JsFn extends TemplateForJsBaseComponent[Raw.ReactFunctionalComponent] {

  type Component[P <: js.Object, CT[-p, +u] <: CtorType[p, u]] = RootComponent[P, CT, Unmounted[P]]
  type Unmounted[P <: js.Object]                               = RootUnmounted[P]
  type Mounted                                                 = Unit

  type RootUnmounted[P <: js.Object] = BaseUnmounted[P, Mounted, P]

  // ===================================================================================================================

  def apply[P <: js.Object, C <: ChildrenArg](rc: Raw.ReactFunctionalComponent)
                                             (implicit s: CtorType.Summoner[P, C]): Component[P, s.CT] =
    rootComponent[P, s.CT, Unmounted[P]](rc, s.pf.rmap(s.summon(rc))(rootUnmounted))(s.pf)

  def apply[P <: js.Object, C <: ChildrenArg](name: String)
                                             (implicit s: CtorType.Summoner[P, C]): Component[P, s.CT] =
    apply[P, C](js.Dynamic.global.selectDynamic(name).asInstanceOf[Raw.ReactFunctionalComponent])(s)

  // ===================================================================================================================

  sealed trait BaseUnmounted[P1, M1, P0 <: js.Object] extends Generic.BaseUnmounted[P1, M1, P0, Mounted] {
    override final type Root = RootUnmounted[P0]
    override def mapUnmountedProps[P2](f: P1 => P2): BaseUnmounted[P2, M1, P0]
    override def mapMounted[M2](f: M1 => M2): BaseUnmounted[P1, M2, P0]

    val raw: Raw.ReactComponentElement
  }

  def rootUnmounted[P <: js.Object](r: Raw.ReactComponentElement): RootUnmounted[P] =
    new RootUnmounted[P] {

      override def root = this
      override def mapUnmountedProps[P2](f: P => P2) = mappedU(this)(f, identity)
      override def mapMounted[M2](f: Mounted => M2) = mappedU(this)(identity, f)

      override val raw = r

      override val reactElement =
        vdom.ReactElement(raw)

      override def key: Option[Key] =
        jsNullToOption(raw.key)

      override def ref: Option[String] =
        // orNullToOption(raw.ref)
        None

      override def props: P =
        raw.props.asInstanceOf[P]

      override def propsChildren: PropsChildren =
        PropsChildren(raw.props.children)

      override def renderIntoDOM(container: Raw.ReactDOM.Container, callback: Callback = Callback.empty): Mounted = {
        val result = Raw.ReactDOM.render(raw, container, callback.toJsFn)

        // Protect against future React change.
        assert(result eq null, "Expected rendered functional component to return null; not " + result)
      }
    }

  private def mappedU[P2, M2, P1, M1, P0 <: js.Object](from: BaseUnmounted[P1, M1, P0])
                                                      (mp: P1 => P2, mm: M1 => M2): BaseUnmounted[P2, M2, P0] =
    new BaseUnmounted[P2, M2, P0] {
      override def root    = from.root
      override val raw           = from.raw
      override def reactElement  = from.reactElement
      override def key           = from.key
      override def ref           = from.ref
      override def props         = mp(from.props)
      override def propsChildren = from.propsChildren
      override def mapUnmountedProps[P3](f: P2 => P3) = mappedU(from)(f compose mp, mm)
      override def mapMounted[M3](f: M2 => M3) = mappedU(from)(mp, f compose mm)
      override def renderIntoDOM(container: Raw.ReactDOM.Container, callback: Callback = Callback.empty) =
        mm(from.renderIntoDOM(container, callback))
    }
}
